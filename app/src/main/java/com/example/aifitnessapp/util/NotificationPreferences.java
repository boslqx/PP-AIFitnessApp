package com.example.aifitnessapp.util;

import android.content.Context;
import android.content.SharedPreferences;

/*
 * NotificationPreferences
 *
 * Wraps SharedPreferences for all notification settings.
 * Read by: SettingsActivity (to populate UI toggles)
 *          NotificationScheduler (to know what to schedule)
 *          BootReceiver (to re-schedule after device reboot)
 *          Each BroadcastReceiver (to check if enabled before posting)
 *
 * DESIGN PRINCIPLE — single source of truth:
 * Every part of the app that needs to know "is the daily reminder on?"
 * reads it from HERE. No magic numbers or duplicated defaults anywhere.
 *
 * DEFAULT VALUES (what the user gets before ever opening Settings):
 *   daily reminder    → OFF (user must opt in, time is personal)
 *   rest day notice   → ON  (low-friction, helpful by default)
 *   weekly plan       → ON  (core feature, should be on by default)
 *   streak reminder   → OFF (can feel naggy, opt-in)
 */
public class NotificationPreferences {

    private static final String PREFS_NAME = "fitai_notifications";

    // ── Keys ──────────────────────────────────────────────────
    // Daily reminder
    public static final String KEY_DAILY_ENABLED = "daily_reminder_enabled";
    public static final String KEY_DAILY_HOUR    = "daily_reminder_hour";
    public static final String KEY_DAILY_MINUTE  = "daily_reminder_minute";

    // Rest day tomorrow
    public static final String KEY_REST_DAY_ENABLED = "rest_day_enabled";

    // Weekly plan ready (Monday)
    public static final String KEY_WEEKLY_ENABLED = "weekly_enabled";

    // Streak reminder
    public static final String KEY_STREAK_ENABLED = "streak_reminder_enabled";

    // Permission asked flag — tracks if we've shown the system permission dialog
    // so we don't ask on every launch
    public static final String KEY_PERMISSION_ASKED = "notification_permission_asked";

    // ── Defaults ──────────────────────────────────────────────
    private static final boolean DEFAULT_DAILY_ENABLED   = false;
    private static final int     DEFAULT_DAILY_HOUR      = 7;   // 7:00 AM
    private static final int     DEFAULT_DAILY_MINUTE    = 0;
    private static final boolean DEFAULT_REST_DAY_ENABLED = true;
    private static final boolean DEFAULT_WEEKLY_ENABLED   = true;
    private static final boolean DEFAULT_STREAK_ENABLED   = false;

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public NotificationPreferences(Context context) {
        prefs  = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // ── DAILY REMINDER ────────────────────────────────────────

    public boolean isDailyEnabled() {
        return prefs.getBoolean(KEY_DAILY_ENABLED, DEFAULT_DAILY_ENABLED);
    }

    public void setDailyEnabled(boolean enabled) {
        editor.putBoolean(KEY_DAILY_ENABLED, enabled).apply();
    }

    public int getDailyHour() {
        return prefs.getInt(KEY_DAILY_HOUR, DEFAULT_DAILY_HOUR);
    }

    public int getDailyMinute() {
        return prefs.getInt(KEY_DAILY_MINUTE, DEFAULT_DAILY_MINUTE);
    }

    public void setDailyTime(int hour, int minute) {
        editor.putInt(KEY_DAILY_HOUR, hour)
                .putInt(KEY_DAILY_MINUTE, minute)
                .apply();
    }

    /*
     * Convenience: get display string for the current daily time.
     * e.g. hour=7, minute=0 → "7:00 AM"
     *      hour=13, minute=30 → "1:30 PM"
     */
    public String getDailyTimeLabel() {
        int h = getDailyHour();
        int m = getDailyMinute();
        String period = h >= 12 ? "PM" : "AM";
        int displayH  = h % 12;
        if (displayH == 0) displayH = 12;
        return String.format(java.util.Locale.getDefault(),
                "%d:%02d %s", displayH, m, period);
    }

    // ── REST DAY REMINDER ─────────────────────────────────────

    public boolean isRestDayEnabled() {
        return prefs.getBoolean(KEY_REST_DAY_ENABLED, DEFAULT_REST_DAY_ENABLED);
    }

    public void setRestDayEnabled(boolean enabled) {
        editor.putBoolean(KEY_REST_DAY_ENABLED, enabled).apply();
    }

    // ── WEEKLY PLAN REMINDER ──────────────────────────────────

    public boolean isWeeklyEnabled() {
        return prefs.getBoolean(KEY_WEEKLY_ENABLED, DEFAULT_WEEKLY_ENABLED);
    }

    public void setWeeklyEnabled(boolean enabled) {
        editor.putBoolean(KEY_WEEKLY_ENABLED, enabled).apply();
    }

    // ── STREAK REMINDER ───────────────────────────────────────

    public boolean isStreakEnabled() {
        return prefs.getBoolean(KEY_STREAK_ENABLED, DEFAULT_STREAK_ENABLED);
    }

    public void setStreakEnabled(boolean enabled) {
        editor.putBoolean(KEY_STREAK_ENABLED, enabled).apply();
    }

    // ── PERMISSION TRACKING ───────────────────────────────────

    public boolean wasPermissionAsked() {
        return prefs.getBoolean(KEY_PERMISSION_ASKED, false);
    }

    public void markPermissionAsked() {
        editor.putBoolean(KEY_PERMISSION_ASKED, true).apply();
    }
}