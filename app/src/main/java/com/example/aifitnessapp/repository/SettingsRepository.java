package com.example.aifitnessapp.repository;

import android.app.Application;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.util.AppExecutors;
import com.example.aifitnessapp.util.SessionManager;
import java.util.List;

/*
 * SettingsRepository — all database operations driven by the Settings screen.
 *
 * RESPONSIBILITIES:
 *  1. Update individual UserPreferences fields (name, weight, goal, activities)
 *  2. Optionally regenerate the current week's plan after profile changes
 *  3. Reset the plan (delete + regenerate fresh)
 *  4. Clear all data (nuclear option)
 *
 * DESIGN FOR PHASE 2C:
 * In Phase 2C, updateFitnessGoal() and updateActivities() will ALSO send
 * a PATCH request to the FastAPI server to sync the profile change.
 * Only this file changes — SettingsViewModel and SettingsActivity don't.
 *
 * CALLBACK INTERFACE:
 * Same pattern as AuthRepository and PlanEditRepository — consistent
 * across the codebase. onSuccess carries no data (settings saves
 * don't need to return a value). onError carries the error message.
 */
public class SettingsRepository {

    public interface SettingsCallback {
        void onSuccess();
        void onError(String message);
    }

    private final FitAIDatabase  db;
    private final SessionManager session;

    public SettingsRepository(Application application) {
        this.db      = FitAIDatabase.getInstance(application);
        this.session = new SessionManager(application);
    }

    // ─────────────────────────────────────────────────────────
    //  LOAD CURRENT PREFERENCES
    //
    //  Called by SettingsViewModel on startup to populate the UI.
    //  Synchronous — called inside diskIO() in the ViewModel.
    // ─────────────────────────────────────────────────────────
    public UserPreferences loadPreferences() {
        return db.userPreferencesDao().getCurrentUserSync();
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE NAME
    //
    //  Simple field update — no plan regeneration needed.
    //  The user's name doesn't affect workout content.
    // ─────────────────────────────────────────────────────────
    public void updateName(String newName, SettingsCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                UserPreferences prefs = db.userPreferencesDao().getCurrentUserSync();
                if (prefs == null) {
                    callback.onError("Profile not found.");
                    return;
                }
                prefs.name     = newName.trim();
                prefs.updatedAt = timestamp();
                db.userPreferencesDao().update(prefs);
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError("Could not save name: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE WEIGHT
    //
    //  Weight affects calorie calculations but not the plan structure.
    //  No plan regeneration needed.
    // ─────────────────────────────────────────────────────────
    public void updateWeight(float newWeightKg, SettingsCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                UserPreferences prefs = db.userPreferencesDao().getCurrentUserSync();
                if (prefs == null) {
                    callback.onError("Profile not found.");
                    return;
                }
                prefs.weightKg  = newWeightKg;
                prefs.updatedAt = timestamp();
                db.userPreferencesDao().update(prefs);
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError("Could not save weight: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE FITNESS GOAL
    //
    //  Fitness goal affects session titles, exercise selection,
    //  and intensity distribution — so it CAN trigger plan regeneration.
    //
    //  applyNow: if true, delete + regenerate this week's plan.
    //            if false, just save the preference — next week's
    //            auto-generated plan will pick it up automatically.
    // ─────────────────────────────────────────────────────────
    public void updateFitnessGoal(String newGoal, boolean applyNow,
                                  SettingsCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                UserPreferences prefs = db.userPreferencesDao().getCurrentUserSync();
                if (prefs == null) {
                    callback.onError("Profile not found.");
                    return;
                }

                prefs.fitnessGoal = newGoal;
                prefs.updatedAt   = timestamp();
                db.userPreferencesDao().update(prefs);

                if (applyNow) {
                    regenerateCurrentWeek(prefs);
                }

                callback.onSuccess();
            } catch (Exception e) {
                callback.onError("Could not update goal: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE ACTIVITIES
    //
    //  Selected activities directly determine which workout types
    //  appear in the plan rotation — high impact change.
    //  Same applyNow pattern as fitness goal.
    // ─────────────────────────────────────────────────────────
    public void updateActivities(String newActivities, boolean applyNow,
                                 SettingsCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                UserPreferences prefs = db.userPreferencesDao().getCurrentUserSync();
                if (prefs == null) {
                    callback.onError("Profile not found.");
                    return;
                }

                prefs.selectedActivities = newActivities;
                prefs.updatedAt          = timestamp();
                db.userPreferencesDao().update(prefs);

                if (applyNow) {
                    regenerateCurrentWeek(prefs);
                }

                callback.onSuccess();
            } catch (Exception e) {
                callback.onError("Could not update activities: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  RESET PLAN
    //
    //  Deletes and fully regenerates this week's plan as if
    //  it's the first time — no log history used for adaptation.
    //  "Fresh start" for the current week.
    //
    //  DIFFERENT FROM regenerateCurrentWeek():
    //  regenerateCurrentWeek() uses recent logs to adapt intensity.
    //  resetPlan() passes null logs → PlanEngine uses base intensity.
    // ─────────────────────────────────────────────────────────
    public void resetPlan(SettingsCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                UserPreferences prefs = db.userPreferencesDao().getCurrentUserSync();
                if (prefs == null) {
                    callback.onError("Profile not found.");
                    return;
                }

                String weekStart = PlanEngine.getCurrentWeekStart();
                int planWeek     = db.plannedWorkoutDao().getLatestPlanWeek(prefs.id);

                // Delete this week's existing plan
                db.plannedWorkoutDao().deletePlanForWeek(prefs.id, planWeek);

                // Regenerate with null logs = no adaptation = fresh intensity
                List<PlannedWorkout> freshPlan = PlanEngine.generateWeekPlan(
                        prefs, null, planWeek, weekStart);

                db.plannedWorkoutDao().insertAll(freshPlan);

                callback.onSuccess();
            } catch (Exception e) {
                callback.onError("Could not reset plan: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  CLEAR ALL DATA
    //
    //  Complete wipe — every table, every row.
    //  Called when user chooses "Clear All Data" from danger zone.
    //
    //  ORDER MATTERS:
    //  Delete child records before parent records to avoid
    //  any foreign key constraint violations (even though we
    //  don't enforce FKs in Room here, good practice).
    //
    //  After DB wipe: clear the session so MainActivity routes
    //  to onboarding on next launch.
    // ─────────────────────────────────────────────────────────
    public void clearAllData(SettingsCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                // Wipe all tables in dependency order
                db.workoutLogDao().deleteAll();
                db.plannedWorkoutDao().deleteAll();
                db.userPreferencesDao().deleteAll();
                db.userDao().deleteAll();

                // Clear session (synchronous SharedPreferences write)
                session.logout();

                callback.onSuccess();
            } catch (Exception e) {
                callback.onError("Could not clear data: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  PRIVATE HELPER: regenerate current week with adaptation
    //
    //  Used by updateFitnessGoal() and updateActivities() when
    //  applyNow = true.
    //
    //  Uses last week's logs so intensity adaptation still applies.
    //  This runs on the background thread already — no need to
    //  dispatch again.
    // ─────────────────────────────────────────────────────────
    private void regenerateCurrentWeek(UserPreferences prefs) {
        String weekStart  = PlanEngine.getCurrentWeekStart();
        int    planWeek   = db.plannedWorkoutDao().getLatestPlanWeek(prefs.id);

        // Get last week's logs for intensity adaptation
        java.util.Calendar cal = java.util.Calendar.getInstance();
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd",
                            java.util.Locale.getDefault());
            java.util.Date monday = sdf.parse(weekStart);
            cal.setTime(monday);
            cal.add(java.util.Calendar.DAY_OF_YEAR, -7);
            String lastWeekStart = sdf.format(cal.getTime());
            List<com.example.aifitnessapp.data.model.WorkoutLog> recentLogs =
                    db.workoutLogDao().getLogsSince(prefs.id, lastWeekStart);

            // Delete current week's plan
            db.plannedWorkoutDao().deletePlanForWeek(prefs.id, planWeek);

            // Regenerate with updated prefs + recent logs
            List<PlannedWorkout> newPlan = PlanEngine.generateWeekPlan(
                    prefs, recentLogs, planWeek, weekStart);

            db.plannedWorkoutDao().insertAll(newPlan);

        } catch (Exception e) {
            // Non-fatal — profile was saved, plan regeneration failed silently
            // The next week's auto-generation will use the new settings
        }
    }

    private String timestamp() {
        return new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date());
    }
}