package com.example.aifitnessapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.aifitnessapp.util.NotificationHelper;
import com.example.aifitnessapp.util.NotificationPreferences;
import com.example.aifitnessapp.util.NotificationScheduler;
import com.example.aifitnessapp.util.SessionManager;

/*
 * BootReceiver
 *
 * Catches BOOT_COMPLETED broadcast when the device finishes starting up.
 * Re-schedules all notification alarms that were cleared by the reboot.
 *
 * WHY THIS IS NECESSARY:
 * AlarmManager alarms are stored in the system's alarm database.
 * This database is cleared on every reboot. Without BootReceiver,
 * all your carefully scheduled alarms simply stop existing after
 * the user restarts their phone.
 *
 * WHAT IT DOES:
 * 1. Checks if a user is logged in (no point scheduling if not)
 * 2. Reads NotificationPreferences to know what was enabled
 * 3. Calls NotificationScheduler to re-schedule each enabled alarm
 * 4. Re-creates notification channels (also cleared on some devices)
 *
 * IMPORTANT: This receiver must be fast. We're reading SharedPreferences
 * (synchronous, instant) and calling AlarmManager (also fast).
 * No database queries, no network calls here.
 *
 * MANIFEST REQUIREMENT:
 * Must declare RECEIVE_BOOT_COMPLETED permission.
 * Must register with action android.intent.action.BOOT_COMPLETED.
 * exported="true" is REQUIRED for system broadcasts to reach it.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Only act on boot completed — ignore other system broadcasts
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        // No user logged in → nothing to schedule
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn()) return;

        // Re-create notification channels (safe to call, no-op if they exist)
        NotificationHelper.ensureChannelsExist(context);

        // Re-schedule each alarm based on saved preferences
        NotificationPreferences prefs = new NotificationPreferences(context);

        // Daily reminder
        if (prefs.isDailyEnabled()) {
            NotificationScheduler.scheduleDailyReminder(
                    context,
                    prefs.getDailyHour(),
                    prefs.getDailyMinute());
        }

        // Rest day reminder
        if (prefs.isRestDayEnabled()) {
            NotificationScheduler.scheduleRestDayReminder(context);
        }

        // Weekly plan reminder
        if (prefs.isWeeklyEnabled()) {
            NotificationScheduler.scheduleWeeklyReminder(context);
        }

        // Streak reminder
        if (prefs.isStreakEnabled()) {
            NotificationScheduler.scheduleStreakReminder(context);
        }
    }
}