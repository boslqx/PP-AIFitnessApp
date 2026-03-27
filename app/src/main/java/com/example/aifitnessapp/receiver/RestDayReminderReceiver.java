package com.example.aifitnessapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.util.NotificationHelper;
import com.example.aifitnessapp.util.NotificationPreferences;
import com.example.aifitnessapp.util.SessionManager;

/*
 * RestDayReminderReceiver
 *
 * Fires every evening at 8:00 PM.
 * Posts a notification ONLY when tomorrow is a rest day in the plan.
 *
 * PURPOSE:
 * Knowing tomorrow is a rest day lets the user plan recovery — better
 * sleep, nutrition, or an optional active recovery walk. It's a
 * positive, informative notification rather than a pushy one.
 *
 * TOMORROW LOOKUP:
 * We get today's day index (0=MON, 6=SUN), add 1, wrap with % 7.
 * If tomorrow is in the same week → same weekStart, next dayOfWeek.
 * If tomorrow is Monday of next week → next weekStart, "MON".
 * We handle both cases below.
 */
public class RestDayReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_REST_DAY_REMINDER =
            "com.example.aifitnessapp.REST_DAY_REMINDER";

    // Day order matching PlanEngine convention
    private static final String[] DAYS = {
            "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"
    };

    @Override
    public void onReceive(Context context, Intent intent) {

        // Guard 1: logged in?
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn()) return;
        int userId = session.getUserId();

        // Guard 2: rest day reminder enabled?
        NotificationPreferences notifPrefs = new NotificationPreferences(context);
        if (!notifPrefs.isRestDayEnabled()) return;

        FitAIDatabase db = FitAIDatabase.getInstance(context);

        // Find tomorrow's day label and week start
        String todayLabel    = PlanEngine.getTodayDayOfWeek();
        int    todayIndex    = dayIndex(todayLabel);
        int    tomorrowIndex = (todayIndex + 1) % 7;
        String tomorrowLabel = DAYS[tomorrowIndex];

        // Determine tomorrow's week start date
        // If today is Sunday (index 6), tomorrow is Monday of the NEXT week
        String tomorrowWeekStart;
        if (todayIndex == 6) {
            // Tomorrow is next Monday — calculate next week start
            tomorrowWeekStart = getNextWeekStart();
        } else {
            // Same week
            tomorrowWeekStart = PlanEngine.getCurrentWeekStart();
        }

        // Look up tomorrow's plan
        PlannedWorkout tomorrowPlan = db.plannedWorkoutDao()
                .getTodayPlanSync(userId, tomorrowWeekStart, tomorrowLabel);

        // Only post if tomorrow is explicitly a rest day
        if (tomorrowPlan == null || !tomorrowPlan.isRestDay) return;

        // Post the notification
        NotificationHelper.postNotification(
                context,
                NotificationHelper.ID_REST,
                NotificationHelper.CHANNEL_REST,
                "😴 Rest day tomorrow",
                "Tomorrow is scheduled recovery. Sleep well tonight!",
                "Your plan has tomorrow as a rest day. "
                        + "Focus on sleep, hydration, and nutrition tonight "
                        + "so you come back stronger."
        );
    }

    // ── Helpers ───────────────────────────────────────────────

    private int dayIndex(String day) {
        for (int i = 0; i < DAYS.length; i++) {
            if (DAYS[i].equals(day)) return i;
        }
        return 0;
    }

    /*
     * Returns the ISO date string of next Monday.
     * Used when today is Sunday and we need to look at next week's plan.
     */
    private String getNextWeekStart() {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date thisMonday = sdf.parse(PlanEngine.getCurrentWeekStart());
            java.util.Calendar cal    = java.util.Calendar.getInstance();
            cal.setTime(thisMonday);
            cal.add(java.util.Calendar.DAY_OF_YEAR, 7);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return PlanEngine.getCurrentWeekStart(); // safe fallback
        }
    }
}