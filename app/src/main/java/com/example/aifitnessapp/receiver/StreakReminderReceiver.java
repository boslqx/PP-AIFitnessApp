package com.example.aifitnessapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.util.NotificationHelper;
import com.example.aifitnessapp.util.NotificationPreferences;
import com.example.aifitnessapp.util.SessionManager;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/*
 * StreakReminderReceiver
 *
 * Fires every day at 7:00 PM.
 * Posts only when the user has NOT completed a workout in the last 48 hours.
 *
 * "48 hours" = yesterday AND today both have no completed log.
 *
 * WHY 7pm specifically?
 * Early enough that the user still has time to do something.
 * Late enough that we've given them the full day first.
 *
 * TONE:
 * Streak reminders can feel naggy. We keep the message encouraging
 * and actionable, not guilt-inducing. One positive nudge per day max.
 */
public class StreakReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_STREAK_REMINDER =
            "com.example.aifitnessapp.STREAK_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {

        // Guard 1: logged in?
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn()) return;
        int userId = session.getUserId();

        // Guard 2: streak reminder enabled?
        NotificationPreferences notifPrefs = new NotificationPreferences(context);
        if (!notifPrefs.isStreakEnabled()) return;

        FitAIDatabase db = FitAIDatabase.getInstance(context);

        // Calculate date 2 days ago
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -2);
        String twoDaysAgo = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        // Check completed workouts in last 2 days
        int recentCompleted = db.workoutLogDao()
                .countCompletedSince(userId, twoDaysAgo);

        // If any workout completed in last 2 days → streak is fine → skip
        if (recentCompleted > 0) return;

        // Also skip if today is a scheduled rest day — resting is correct
        String weekStart = PlanEngine.getCurrentWeekStart();
        String today     = PlanEngine.getTodayDayOfWeek();
        com.example.aifitnessapp.data.model.PlannedWorkout todayPlan =
                db.plannedWorkoutDao().getTodayPlanSync(userId, weekStart, today);
        if (todayPlan != null && todayPlan.isRestDay) return;

        // All guards passed — streak is genuinely at risk
        NotificationHelper.postNotification(
                context,
                NotificationHelper.ID_STREAK,
                NotificationHelper.CHANNEL_STREAK,
                "🔥 Keep your streak alive!",
                "You haven't logged a workout in 2 days. Even a short session counts.",
                "You haven't logged a workout in 2 days. "
                        + "Even a 20-minute session keeps your momentum going. "
                        + "Open the app and log something today!"
        );
    }
}