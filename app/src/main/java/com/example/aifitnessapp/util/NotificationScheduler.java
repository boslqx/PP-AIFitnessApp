package com.example.aifitnessapp.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.example.aifitnessapp.receiver.DailyReminderReceiver;
import com.example.aifitnessapp.receiver.RestDayReminderReceiver;
import com.example.aifitnessapp.receiver.StreakReminderReceiver;
import com.example.aifitnessapp.receiver.WeeklyReminderReceiver;
import java.util.Calendar;

public class NotificationScheduler {

    private static final int RC_DAILY  = 2001;
    private static final int RC_REST   = 2002;
    private static final int RC_WEEKLY = 2003;
    private static final int RC_STREAK = 2004;

    // ── DAILY REMINDER ────────────────────────────────────────
    public static void scheduleDailyReminder(Context context, int hour, int minute) {
        AlarmManager am = getAlarmManager(context);
        if (am == null) return;
        PendingIntent pi = buildPI(context, RC_DAILY,
                DailyReminderReceiver.ACTION_DAILY_REMINDER,
                DailyReminderReceiver.class);
        am.cancel(pi);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    public static void cancelDailyReminder(Context context) {
        AlarmManager am = getAlarmManager(context);
        if (am == null) return;
        am.cancel(buildPI(context, RC_DAILY,
                DailyReminderReceiver.ACTION_DAILY_REMINDER,
                DailyReminderReceiver.class));
    }

    // ── REST DAY REMINDER (8 PM daily) ────────────────────────
    public static void scheduleRestDayReminder(Context context) {
        AlarmManager am = getAlarmManager(context);
        if (am == null) return;
        PendingIntent pi = buildPI(context, RC_REST,
                RestDayReminderReceiver.ACTION_REST_DAY_REMINDER,
                RestDayReminderReceiver.class);
        am.cancel(pi);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 20);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    public static void cancelRestDayReminder(Context context) {
        AlarmManager am = getAlarmManager(context);
        if (am == null) return;
        am.cancel(buildPI(context, RC_REST,
                RestDayReminderReceiver.ACTION_REST_DAY_REMINDER,
                RestDayReminderReceiver.class));
    }

    // ── WEEKLY PLAN REMINDER (Monday 8 AM) ───────────────────
    public static void scheduleWeeklyReminder(Context context) {
        AlarmManager am = getAlarmManager(context);
        if (am == null) return;
        PendingIntent pi = buildPI(context, RC_WEEKLY,
                WeeklyReminderReceiver.ACTION_WEEKLY_REMINDER,
                WeeklyReminderReceiver.class);
        am.cancel(pi);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                getNextMondayAt8am(), AlarmManager.INTERVAL_DAY * 7, pi);
    }

    public static void cancelWeeklyReminder(Context context) {
        AlarmManager am = getAlarmManager(context);
        if (am == null) return;
        am.cancel(buildPI(context, RC_WEEKLY,
                WeeklyReminderReceiver.ACTION_WEEKLY_REMINDER,
                WeeklyReminderReceiver.class));
    }

    // ── STREAK REMINDER (7 PM daily) ─────────────────────────
    public static void scheduleStreakReminder(Context context) {
        AlarmManager am = getAlarmManager(context);
        if (am == null) return;
        PendingIntent pi = buildPI(context, RC_STREAK,
                StreakReminderReceiver.ACTION_STREAK_REMINDER,
                StreakReminderReceiver.class);
        am.cancel(pi);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 19);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    public static void cancelStreakReminder(Context context) {
        AlarmManager am = getAlarmManager(context);
        if (am == null) return;
        am.cancel(buildPI(context, RC_STREAK,
                StreakReminderReceiver.ACTION_STREAK_REMINDER,
                StreakReminderReceiver.class));
    }

    // ── SCHEDULE ALL FROM SAVED PREFERENCES ──────────────────
    /*
     * Called on first app open after login and after BootReceiver fires.
     * Reads what the user had enabled and restores all active alarms.
     */
    public static void scheduleAllFromPreferences(Context context) {
        NotificationPreferences prefs = new NotificationPreferences(context);
        if (prefs.isDailyEnabled())
            scheduleDailyReminder(context, prefs.getDailyHour(), prefs.getDailyMinute());
        if (prefs.isRestDayEnabled())
            scheduleRestDayReminder(context);
        if (prefs.isWeeklyEnabled())
            scheduleWeeklyReminder(context);
        if (prefs.isStreakEnabled())
            scheduleStreakReminder(context);
    }

    // ── Helpers ───────────────────────────────────────────────

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private static <T extends android.content.BroadcastReceiver>
    PendingIntent buildPI(Context ctx, int rc, String action, Class<T> cls) {
        Intent i = new Intent(ctx, cls);
        i.setAction(action);
        return PendingIntent.getBroadcast(ctx, rc, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static long getNextMondayAt8am() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int days = dow == Calendar.MONDAY
                ? (cal.getTimeInMillis() > System.currentTimeMillis() ? 0 : 7)
                : (Calendar.MONDAY - dow + 7) % 7;
        if (days == 0) days = 7;
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTimeInMillis();
    }
}