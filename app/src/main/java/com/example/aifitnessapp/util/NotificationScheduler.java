package com.example.aifitnessapp.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.example.aifitnessapp.receiver.WeeklyReminderReceiver;
import java.util.Calendar;

/*
 * NotificationScheduler
 *
 * Utility class that sets up the recurring Monday morning alarm.
 * Call scheduleWeeklyReminder() once — typically from MainActivity
 * on first launch, or after onboarding completes.
 *
 * IDEMPOTENT DESIGN:
 * If the alarm is already scheduled and we call this again (e.g. app
 * reopened), we cancel the old one first and reschedule. This ensures
 * the alarm time is always fresh and correct.
 * "Idempotent" means calling it multiple times has the same effect
 * as calling it once — no duplicates, no side effects.
 *
 * WHY NOT WorkManager?
 * WorkManager is Google's recommended solution for deferrable background
 * work. For a learning project, AlarmManager is more transparent —
 * you see exactly how Android's alarm system works. WorkManager adds
 * abstraction that hides these fundamentals. We'll migrate in Phase 2B
 * when we do the full notifications system.
 */
public class NotificationScheduler {

    // Request code — identifies this specific PendingIntent.
    // If you later add a second alarm type, give it a different request code.
    private static final int REQUEST_CODE_WEEKLY = 2001;

    /*
     * Schedule a weekly notification every Monday at 8:00 AM.
     *
     * HOW IT FINDS NEXT MONDAY:
     * 1. Get today's Calendar instance
     * 2. Set time to 8:00 AM
     * 3. Find how many days until Monday
     * 4. If today IS Monday but 8am has passed, schedule for next Monday
     * 5. Set the alarm to repeat every 7 days (one week in milliseconds)
     */
    public static void scheduleWeeklyReminder(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Build the PendingIntent that fires WeeklyReminderReceiver
        Intent intent = new Intent(context, WeeklyReminderReceiver.class);
        intent.setAction(WeeklyReminderReceiver.ACTION_WEEKLY_REMINDER);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_WEEKLY,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Cancel any existing alarm with this PendingIntent first
        alarmManager.cancel(pendingIntent);

        // Calculate next Monday at 8:00 AM
        long triggerTime = getNextMondayAt8am();

        // One week in milliseconds
        long oneWeekMs = 7L * 24 * 60 * 60 * 1000;

        /*
         * setInexactRepeating():
         * - triggerTime: when to fire the first alarm
         * - interval: how often to repeat (AlarmManager.INTERVAL_DAY * 7
         *   is the built-in constant for one week)
         * - pendingIntent: what to fire
         *
         * INTERVAL_DAY is 86,400,000ms. Multiplied by 7 = one week.
         * We use AlarmManager.INTERVAL_DAY * 7 to use the system constant
         * which Android can optimize (batch with other weekly alarms).
         */
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,   // wake device even if screen is off
                triggerTime,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
        );
    }

    /*
     * Cancel the weekly reminder.
     * Called if the user disables notifications in Settings (Phase 2B).
     */
    public static void cancelWeeklyReminder(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, WeeklyReminderReceiver.class);
        intent.setAction(WeeklyReminderReceiver.ACTION_WEEKLY_REMINDER);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_WEEKLY,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }

    /*
     * Calculates the timestamp for next Monday at 8:00 AM.
     *
     * EDGE CASE HANDLING:
     * - If today is Monday AND 8am hasn't passed: return today at 8am
     * - If today is Monday AND 8am has passed: return next Monday at 8am
     * - Any other day: find days until Monday, add to today at 8am
     */
    private static long getNextMondayAt8am() {
        Calendar cal = Calendar.getInstance();

        // Set to 8:00:00 AM today
        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Find how many days until next Monday
        int dayOfWeek     = cal.get(Calendar.DAY_OF_WEEK);
        int daysUntilMon;

        if (dayOfWeek == Calendar.MONDAY) {
            // Today is Monday — check if 8am is still in the future
            if (cal.getTimeInMillis() > System.currentTimeMillis()) {
                daysUntilMon = 0; // still today
            } else {
                daysUntilMon = 7; // already past 8am, schedule next Monday
            }
        } else {
            // Days until next Monday: (MON=2, so we calculate forward)
            // Calendar.MONDAY = 2, Calendar.SUNDAY = 1
            daysUntilMon = (Calendar.MONDAY - dayOfWeek + 7) % 7;
            if (daysUntilMon == 0) daysUntilMon = 7;
        }

        cal.add(Calendar.DAY_OF_YEAR, daysUntilMon);
        return cal.getTimeInMillis();
    }
}