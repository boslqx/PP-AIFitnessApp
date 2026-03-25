package com.example.aifitnessapp.engine;

import android.content.Context;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.UserPreferences;
import com.example.aifitnessapp.data.model.WorkoutLog;
import com.example.aifitnessapp.repository.PlanRepository;
import com.example.aifitnessapp.util.AppExecutors;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * WeeklyPlanManager
 *
 * Single responsibility: decide whether a new weekly plan is needed,
 * and execute whichever generation strategy the user picks.
 *
 * CALLED FROM: MainActivity (on every app open)
 *
 * TWO STRATEGIES:
 *
 * 1. GENERATE_NEW — runs PlanEngine with last week's logs.
 *    Adapts intensity based on how the user performed.
 *    This is the smart AI path.
 *
 * 2. COPY_LAST_WEEK — duplicates last week's PlannedWorkout rows
 *    with updated dates. User gets the exact same schedule again.
 *    Useful when the user is happy with last week's structure.
 *
 * CALLBACK PATTERN:
 * All database operations run on a background thread.
 * Results are delivered back via the WeeklyPlanCallback interface,
 * which the Activity implements to react on the UI thread.
 */
public class WeeklyPlanManager {

    // ── Callback interface ────────────────────────────────────
    /*
     * The Activity passes an implementation of this interface.
     * needsDialog() is called on the MAIN thread — safe to show UI.
     * onComplete() is called on the MAIN thread — safe to navigate.
     * onError() is called on the MAIN thread — safe to show toast.
     */
    public interface WeeklyPlanCallback {
        // Called when we detect a new week needs a plan — show the dialog
        void needsDialog();
        // Called after plan is generated/copied successfully
        void onComplete();
        // Called if something goes wrong
        void onError(String message);
    }

    private final Context       context;
    private final FitAIDatabase db;

    public WeeklyPlanManager(Context context) {
        // Use application context to avoid memory leaks.
        // Activity contexts get destroyed on rotation — application context lives forever.
        this.context = context.getApplicationContext();
        this.db      = FitAIDatabase.getInstance(this.context);
    }

    // ─────────────────────────────────────────────────────────
    //  CHECK IF DIALOG IS NEEDED
    //
    //  Called every time MainActivity opens.
    //  Runs the check on a background thread (database query).
    //  Calls back on the main thread.
    //
    //  CONDITIONS for showing the dialog:
    //  1. A user profile exists (app has been onboarded)
    //  2. No plan exists for the current week yet
    //
    //  Note: we do NOT check if it's Monday specifically.
    //  WHY? Because the user might open the app on Tuesday if they
    //  missed Monday. We still want them to get a plan for the week.
    //  The notification fires on Monday, but the dialog appears
    //  whenever the plan is missing — regardless of day.
    // ─────────────────────────────────────────────────────────
    public void checkAndPromptIfNeeded(int userId,
                                       android.os.Handler mainHandler,
                                       WeeklyPlanCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            UserPreferences prefs = db.userPreferencesDao().getCurrentUserSync();
            if (prefs == null) {
                // No user yet — onboarding hasn't happened, nothing to do
                return;
            }

            String weekStart = PlanEngine.getCurrentWeekStart();
            List<PlannedWorkout> existing =
                    db.plannedWorkoutDao().getWeekPlanSync(userId, weekStart);

            boolean planMissing = existing == null || existing.isEmpty();

            if (planMissing) {
                // Post back to main thread so the Activity can show a dialog
                mainHandler.post(callback::needsDialog);
            }
            // If plan exists, do nothing — MainActivity routes normally
        });
    }

    // ─────────────────────────────────────────────────────────
    //  STRATEGY 1: GENERATE NEW PLAN (AI-adapted)
    //
    //  Uses last week's logs to adapt intensity.
    //  This is the same logic PlanRepository.generateNextWeekPlan()
    //  uses — we call it directly here so WeeklyPlanManager owns
    //  the full weekly flow in one place.
    // ─────────────────────────────────────────────────────────
    public void generateNewPlan(int userId,
                                android.os.Handler mainHandler,
                                WeeklyPlanCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                UserPreferences prefs = db.userPreferencesDao().getCurrentUserSync();
                if (prefs == null) {
                    mainHandler.post(() -> callback.onError("Profile not found."));
                    return;
                }

                String weekStart = PlanEngine.getCurrentWeekStart();

                // Get last week's logs for intensity adaptation
                String lastWeekStart = getLastWeekStart();
                List<WorkoutLog> recentLogs =
                        db.workoutLogDao().getLogsSince(userId, lastWeekStart);

                // Determine plan week number
                int lastWeek = db.plannedWorkoutDao().getLatestPlanWeek(userId);
                int nextWeek = lastWeek + 1;

                // Generate using PlanEngine — the AI rules-based engine
                List<PlannedWorkout> plan = PlanEngine.generateWeekPlan(
                        prefs, recentLogs, nextWeek, weekStart);

                db.plannedWorkoutDao().insertAll(plan);

                mainHandler.post(callback::onComplete);

            } catch (Exception e) {
                mainHandler.post(() ->
                        callback.onError("Could not generate plan: " + e.getMessage()));
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  STRATEGY 2: COPY LAST WEEK'S PLAN
    //
    //  Finds last week's PlannedWorkout rows, duplicates them
    //  with this week's dates. The workout content (exercises,
    //  intensity, coach notes) stays identical.
    //
    //  HOW COPYING WORKS IN ROOM:
    //  We load the old rows, clear their 'id' (set to 0 so Room
    //  auto-generates new ones), update weekStartDate and planWeek,
    //  then insert them as new rows. The old rows remain untouched.
    //
    //  WHY clear the id?
    //  Room's @PrimaryKey(autoGenerate = true) only generates a new
    //  id when id == 0. If we insert a row with an existing id,
    //  Room would overwrite the original row (REPLACE strategy).
    //  We want NEW rows, not overwrites.
    // ─────────────────────────────────────────────────────────
    public void copyLastWeekPlan(int userId,
                                 android.os.Handler mainHandler,
                                 WeeklyPlanCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                String lastWeekStart = getLastWeekStart();
                String thisWeekStart = PlanEngine.getCurrentWeekStart();

                List<PlannedWorkout> lastWeek =
                        db.plannedWorkoutDao().getWeekPlanSync(userId, lastWeekStart);

                if (lastWeek == null || lastWeek.isEmpty()) {
                    // No last week to copy — fall back to generating fresh
                    generateNewPlan(userId, mainHandler, callback);
                    return;
                }

                int lastPlanWeek = db.plannedWorkoutDao().getLatestPlanWeek(userId);
                int newPlanWeek  = lastPlanWeek + 1;

                // Build copies with new dates
                List<PlannedWorkout> copies = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String now = sdf.format(new Date());

                for (PlannedWorkout original : lastWeek) {
                    PlannedWorkout copy = new PlannedWorkout();

                    // id = 0 → Room will auto-generate a new primary key
                    copy.id             = 0;
                    copy.userId         = original.userId;
                    copy.planWeek       = newPlanWeek;
                    copy.dayOfWeek      = original.dayOfWeek;
                    copy.weekStartDate  = thisWeekStart;   // ← updated to this week

                    // Copy all workout content unchanged
                    copy.activityType   = original.activityType;
                    copy.sessionTitle   = original.sessionTitle;
                    copy.sessionDetail  = original.sessionDetail;
                    copy.coachNote      = "Repeating last week's plan — "
                            + "keep up the consistency!";
                    copy.intensityLevel = original.intensityLevel;
                    copy.durationMinutes= original.durationMinutes;
                    copy.isRestDay      = original.isRestDay;
                    copy.createdAt      = now;

                    copies.add(copy);
                }

                db.plannedWorkoutDao().insertAll(copies);

                mainHandler.post(callback::onComplete);

            } catch (Exception e) {
                mainHandler.post(() ->
                        callback.onError("Could not copy plan: " + e.getMessage()));
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  DATE HELPER: get the Monday of LAST week
    //
    //  PlanEngine.getCurrentWeekStart() gives us THIS Monday.
    //  Subtracting 7 days gives us LAST Monday.
    // ─────────────────────────────────────────────────────────
    private String getLastWeekStart() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault());
            String thisMonday = PlanEngine.getCurrentWeekStart();
            Date   date       = sdf.parse(thisMonday);
            Calendar cal      = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_YEAR, -7);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return "2020-01-01"; // safe fallback
        }
    }

    // ─────────────────────────────────────────────────────────
    //  STATIC HELPER: is today Monday?
    //
    //  Used by the notification scheduler to decide whether
    //  to fire the Monday reminder.
    // ─────────────────────────────────────────────────────────
    public static boolean isTodayMonday() {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                == Calendar.MONDAY;
    }
}