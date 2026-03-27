package com.example.aifitnessapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.PlannedWorkout;
import com.example.aifitnessapp.data.model.WorkoutLog;
import com.example.aifitnessapp.engine.PlanEngine;
import com.example.aifitnessapp.util.NotificationHelper;
import com.example.aifitnessapp.util.NotificationPreferences;
import com.example.aifitnessapp.util.SessionManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * DailyReminderReceiver
 *
 * Fires at the user's chosen time every day.
 *
 * CONDITION CHECKS before posting (short-circuit logic):
 *  1. Is the user logged in?          → No  → skip
 *  2. Is daily reminder still enabled? → No  → skip (user may have disabled)
 *  3. Is today a rest day in the plan? → Yes → skip (don't remind on rest days)
 *  4. Has the user already logged today? → Yes → skip (already done!)
 *  Only if ALL pass → post the notification.
 *
 * WHY CHECK INSIDE THE RECEIVER?
 * AlarmManager schedules based on time only — it can't check conditions.
 * The receiver is where we apply the "should I actually ring?" logic.
 * This is called the "guard clause" pattern — check all exit conditions
 * at the top, main logic at the bottom.
 *
 * IMPORTANT: BroadcastReceivers must complete in under 10 seconds.
 * Our DB queries are lightweight (single row lookups) — well within limit.
 * For heavy work inside a receiver, use a Worker (Phase 2C).
 */
public class DailyReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_DAILY_REMINDER =
            "com.example.aifitnessapp.DAILY_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {

        // Guard 1: Is a user logged in?
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn()) return;
        int userId = session.getUserId();

        // Guard 2: Is the daily reminder still enabled?
        // (User might have turned it off since the alarm was set)
        NotificationPreferences notifPrefs = new NotificationPreferences(context);
        if (!notifPrefs.isDailyEnabled()) return;

        FitAIDatabase db = FitAIDatabase.getInstance(context);

        // Guard 3: Is today a rest day?
        String weekStart = PlanEngine.getCurrentWeekStart();
        String today     = PlanEngine.getTodayDayOfWeek();
        PlannedWorkout todayPlan =
                db.plannedWorkoutDao().getTodayPlanSync(userId, weekStart, today);

        if (todayPlan == null) return;        // no plan loaded yet
        if (todayPlan.isRestDay) return;      // rest day — don't remind

        // Guard 4: Has the user already logged today?
        String todayDate = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()).format(new Date());
        WorkoutLog existingLog =
                db.workoutLogDao().getLogForDate(userId, todayDate);
        if (existingLog != null) return;      // already logged — skip

        // All guards passed — post the notification
        String title  = "💪 Time to work out!";
        String body   = todayPlan.sessionTitle != null
                ? todayPlan.sessionTitle : "Your workout is waiting.";
        String bigText = todayPlan.sessionDetail != null
                ? todayPlan.sessionDetail
                : "Open the app to see today's session.";

        NotificationHelper.postNotification(
                context,
                NotificationHelper.ID_DAILY,
                NotificationHelper.CHANNEL_DAILY,
                title, body, bigText);
    }
}