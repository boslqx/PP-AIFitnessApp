package com.example.aifitnessapp.repository;

import android.app.Application;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.db.dao.PlannedWorkoutDao;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.util.AppExecutors;
import java.util.Arrays;
import java.util.List;

/*
 * PlanEditRepository — handles all user-initiated edits to a planned week.
 *
 * DESIGN PRINCIPLE: Every public method in this class:
 *   1. Runs its database work on AppExecutors.diskIO() (background thread)
 *   2. Calls back on the result via an EditCallback interface
 *   3. Never touches the UI directly
 *
 * WHY CALLBACKS instead of LiveData here?
 *   Edit operations are one-shot actions (not ongoing streams of data).
 *   LiveData is designed for ongoing observation. For "did the save succeed?"
 *   a simple callback is cleaner and easier to understand.
 */
public class PlanEditRepository {

    // ── Callback interface ────────────────────────────────────
    /*
     * The ViewModel passes in an EditCallback when it calls any edit method.
     * When the background work finishes, we call onSuccess() or onError().
     * The ViewModel then updates its MutableLiveData, which the Activity observes.
     *
     * This pattern is called "Inversion of Control" — the Repository doesn't
     * decide what happens after saving; the caller (ViewModel) decides.
     */
    public interface EditCallback {
        void onSuccess();
        void onError(String message);
    }

    private final FitAIDatabase db;
    private final PlannedWorkoutDao dao;

    public PlanEditRepository(Application application) {
        this.db  = FitAIDatabase.getInstance(application);
        this.dao = db.plannedWorkoutDao();
    }

    // ─────────────────────────────────────────────────────────
    //  OPERATION 1: SWAP TWO DAYS
    //
    //  User picks Day A (e.g. Monday gym) and Day B (e.g. Thursday run).
    //  Result: Monday gets Thursday's workout, Thursday gets Monday's workout.
    //
    //  HOW IT WORKS IN ROOM:
    //  PlannedWorkout rows are identified by their 'id' (primary key).
    //  The 'id' stays fixed — we swap the *content* fields (activityType,
    //  sessionTitle, sessionDetail, etc.) between the two rows.
    //  The 'dayOfWeek' field also stays fixed — it identifies which day
    //  each row represents. So we're swapping all the workout content,
    //  NOT the day labels.
    //
    //  WHY updateAll()? Both rows must change together. If the app crashes
    //  between two separate updates, we'd end up with Monday having both
    //  workouts or neither. updateAll() sends both to Room in one batch.
    // ─────────────────────────────────────────────────────────
    public void swapDays(int planIdA, int planIdB, EditCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                PlannedWorkout a = dao.getByIdSync(planIdA);
                PlannedWorkout b = dao.getByIdSync(planIdB);

                if (a == null || b == null) {
                    callback.onError("Could not load workouts to swap.");
                    return;
                }

                // Capture all content fields from A
                String tmpActivity    = a.activityType;
                String tmpTitle       = a.sessionTitle;
                String tmpDetail      = a.sessionDetail;
                String tmpCoachNote   = a.coachNote;
                int    tmpIntensity   = a.intensityLevel;
                int    tmpDuration    = a.durationMinutes;
                boolean tmpIsRest     = a.isRestDay;

                // Write B's content into A's row
                a.activityType    = b.activityType;
                a.sessionTitle    = b.sessionTitle;
                a.sessionDetail   = b.sessionDetail;
                a.coachNote       = b.coachNote;
                a.intensityLevel  = b.intensityLevel;
                a.durationMinutes = b.durationMinutes;
                a.isRestDay       = b.isRestDay;

                // Write A's original content into B's row
                b.activityType    = tmpActivity;
                b.sessionTitle    = tmpTitle;
                b.sessionDetail   = tmpDetail;
                b.coachNote       = tmpCoachNote;
                b.intensityLevel  = tmpIntensity;
                b.durationMinutes = tmpDuration;
                b.isRestDay       = tmpIsRest;

                // Save both in one call
                dao.updateAll(Arrays.asList(a, b));
                callback.onSuccess();

            } catch (Exception e) {
                callback.onError("Swap failed: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  OPERATION 2: CHANGE ACTIVITY TYPE
    //
    //  User changes e.g. Monday GYM → RUNNING.
    //  We regenerate the session fields using PlanEngine's static
    //  helper methods — the same logic used during plan generation.
    //
    //  WHY REUSE PlanEngine? DRY principle — "Don't Repeat Yourself."
    //  The rules for what a Running session looks like live in one place.
    //  If we later improve those rules, both plan generation AND edits
    //  benefit automatically.
    // ─────────────────────────────────────────────────────────
    public void changeActivity(int planId, String newActivityType,
                               UserPreferences prefs, EditCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                PlannedWorkout plan = dao.getByIdSync(planId);
                if (plan == null) {
                    callback.onError("Could not load workout.");
                    return;
                }

                // Use PlanEngine's static methods to regenerate content.
                // We pass the plan's existing intensity and week number
                // so the session stays consistent with the user's progression.
                plan.activityType    = newActivityType;
                plan.sessionTitle    = buildTitle(newActivityType,
                        plan.intensityLevel,
                        prefs.fitnessGoal);
                plan.sessionDetail   = buildDetail(newActivityType, prefs);
                plan.durationMinutes = resolveDuration(newActivityType,
                        plan.intensityLevel);
                plan.isRestDay       = false;
                plan.coachNote       = "Activity changed by you. "
                        + "Give it your best effort!";

                dao.update(plan);
                callback.onSuccess();

            } catch (Exception e) {
                callback.onError("Change failed: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  OPERATION 3: ADD WORKOUT TO REST DAY
    //
    //  User decides they want to work out on a planned rest day.
    //  We flip isRestDay → false and populate the session fields.
    //
    //  The activity type comes from the user's choice (after seeing
    //  AI suggestions). This method receives the final chosen activity.
    // ─────────────────────────────────────────────────────────
    public void addWorkoutToRestDay(int planId, String activityType,
                                    UserPreferences prefs, EditCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                PlannedWorkout plan = dao.getByIdSync(planId);
                if (plan == null) {
                    callback.onError("Could not load rest day.");
                    return;
                }

                // Base intensity on the user's fitness level.
                // Since this is a bonus session (not AI-planned),
                // we default to a moderate intensity to avoid overtraining.
                int intensity = resolveBaseIntensity(prefs.fitnessLevel);

                plan.isRestDay       = false;
                plan.activityType    = activityType;
                plan.intensityLevel  = intensity;
                plan.durationMinutes = resolveDuration(activityType, intensity);
                plan.sessionTitle    = buildTitle(activityType, intensity,
                        prefs.fitnessGoal);
                plan.sessionDetail   = buildDetail(activityType, prefs);
                plan.coachNote       = "Extra session added by you — "
                        + "listen to your body and don't overdo it.";

                dao.update(plan);
                callback.onSuccess();

            } catch (Exception e) {
                callback.onError("Could not add workout: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  OPERATION 4: CANCEL A WORKOUT DAY
    //
    //  User cancels a planned workout. The day becomes a Rest day.
    //  We clear all session fields to keep the data clean.
    //
    //  Note: this is different from SKIPPING (which is a log action
    //  after the fact). Cancelling is a forward-looking plan change.
    // ─────────────────────────────────────────────────────────
    public void cancelWorkout(int planId, EditCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                PlannedWorkout plan = dao.getByIdSync(planId);
                if (plan == null) {
                    callback.onError("Could not load workout.");
                    return;
                }

                plan.isRestDay       = true;
                plan.activityType    = "REST";
                plan.sessionTitle    = "Rest Day";
                plan.sessionDetail   = "Recovery is part of the plan. "
                        + "Sleep well, stay hydrated.";
                plan.coachNote       = "You cancelled this session. "
                        + "That's okay — rest when you need it.";
                plan.intensityLevel  = 0;
                plan.durationMinutes = 0;

                dao.update(plan);
                callback.onSuccess();

            } catch (Exception e) {
                callback.onError("Cancel failed: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  HELPER METHODS
    //
    //  These mirror PlanEngine's private methods.
    //  We duplicate them here because PlanEngine's methods are private.
    //
    //  DESIGN NOTE: In a larger project you'd move these to a shared
    //  utility class. For this learning project, keeping them here
    //  is fine and makes the Repository self-contained.
    // ─────────────────────────────────────────────────────────

    private int resolveBaseIntensity(String fitnessLevel) {
        switch (fitnessLevel) {
            case "BEGINNER":     return 2;
            case "INTERMEDIATE": return 3;
            case "ADVANCED":     return 4;
            default:             return 2;
        }
    }

    private int resolveDuration(String activity, int intensity) {
        int base;
        switch (activity) {
            case "GYM":        base = 60; break;
            case "RUNNING":    base = 30; break;
            case "BOULDERING": base = 75; break;
            case "CYCLING":    base = 45; break;
            case "SWIMMING":   base = 40; break;
            case "YOGA":       base = 40; break;
            case "HOME":       base = 35; break;
            case "SPORTS":     base = 60; break;
            default:           base = 40;
        }
        return base + (intensity - 3) * 5;
    }

    private String buildTitle(String activity, int intensity, String goal) {
        String intensityWord = intensity <= 2 ? "Light"
                : intensity == 3 ? "Moderate" : "Hard";
        switch (activity) {
            case "GYM":
                switch (goal) {
                    case "BUILD_MUSCLE": return intensityWord + " Gym — Strength";
                    case "LOSE_FAT":     return intensityWord + " Gym — Circuit";
                    default:             return intensityWord + " Gym Session";
                }
            case "RUNNING":    return intensityWord + " Run";
            case "BOULDERING": return "Bouldering Session";
            case "CYCLING":    return intensityWord + " Cycle";
            case "SWIMMING":   return intensityWord + " Swim";
            case "YOGA":       return "Yoga & Stretching";
            case "HOME":       return intensityWord + " Home Workout";
            case "SPORTS":     return "Sports Session";
            default:           return intensityWord + " Workout";
        }
    }

    private String buildDetail(String activity, UserPreferences prefs) {
        switch (activity) {
            case "GYM":        return buildGymDetail(prefs);
            case "RUNNING":
                return prefs.tracksRunningPace
                        ? "Easy pace run. Target: conversational effort. Track your pace."
                        : "Easy pace run. Go at a comfortable, conversational effort.";
            case "BOULDERING":
                return "Warm up on V0–V1 problems. Spend 60% time on your project grade. "
                        + "Cool down with easy problems and wrist stretches.";
            case "CYCLING":
                return "Steady pace ride. Keep cadence around 80–90 RPM. "
                        + "Focus on consistent effort, not speed.";
            case "SWIMMING":
                return "Warm up 200m easy. Main set: 4×100m at moderate pace. "
                        + "Cool down 100m easy. Focus on stroke technique.";
            case "YOGA":
                return "Full body flow. Focus on hip flexors, hamstrings, and thoracic spine. "
                        + "Hold each pose 30–60 seconds. Breathe deeply.";
            case "HOME":       return buildHomeDetail(prefs.fitnessGoal);
            case "SPORTS":
                return prefs.sports != null && !prefs.sports.isEmpty()
                        ? "Play " + prefs.sports + ". Focus on movement quality and enjoyment."
                        : "Sports session. Play at your preferred intensity.";
            default:
                return "Complete your planned workout at the assigned intensity.";
        }
    }

    private String buildGymDetail(UserPreferences prefs) {
        String equipment = prefs.gymEquipment != null ? prefs.gymEquipment : "FULL_GYM";
        switch (prefs.fitnessGoal) {
            case "BUILD_MUSCLE":
                switch (equipment) {
                    case "DUMBBELLS":
                        return "3×10 Dumbbell press · 3×12 DB rows · 3×10 DB shoulder press "
                                + "· 3×15 DB lunges · 3×12 DB bicep curl · Core 3×20";
                    case "BARBELL_RACK":
                        return "4×5 Squat · 4×5 Bench press · 3×8 Barbell row "
                                + "· 3×8 Overhead press · 3×10 RDL";
                    case "MACHINES":
                        return "3×12 Chest press machine · 3×12 Lat pulldown · 3×12 Leg press "
                                + "· 3×15 Shoulder press · 3×15 Seated row";
                    default:
                        return "4×5 Squat · 4×5 Bench press · 3×8 Barbell row "
                                + "· 3×10 Incline DB press · 3×12 Cable row · Core 3×20";
                }
            case "LOSE_FAT":
                switch (equipment) {
                    case "DUMBBELLS":
                        return "Circuit ×3: DB thrusters ×15 · Renegade rows ×10 "
                                + "· DB step-ups ×12 · Plank 45s · Rest 60s";
                    default:
                        return "Circuit ×3: Barbell complex (clean+press+squat) ×8 "
                                + "· Treadmill intervals 30s on/30s off ×8 · Core 3×20";
                }
            default:
                return "Full body: 3×15 each — squats, push-ups, rows, shoulder press, "
                        + "lunges. Keep rest under 60s. Heart rate moderate throughout.";
        }
    }

    private String buildHomeDetail(String goal) {
        switch (goal) {
            case "BUILD_MUSCLE":
                return "4×10 Push-ups · 4×12 Glute bridges · 3×10 Pike push-ups "
                        + "· 3×15 Reverse lunges · 3×20 Superman holds";
            case "LOSE_FAT":
                return "HIIT circuit ×4: Jump squats ×15 · Push-ups ×10 · High knees 30s "
                        + "· Plank 30s · Rest 45s between rounds";
            default:
                return "3×12: Squats · Push-ups · Hip hinges · Mountain climbers · "
                        + "Plank hold 30s. Focus on form over speed.";
        }
    }
}