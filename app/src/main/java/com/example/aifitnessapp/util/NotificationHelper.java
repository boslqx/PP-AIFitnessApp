package com.example.aifitnessapp.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.aifitnessapp.MainActivity;

/*
 * NotificationHelper
 *
 * Single place to create channels and post notifications.
 * All four receivers call this instead of duplicating builder code.
 *
 * CHANNELS:
 * Android 8.0+ requires every notification to belong to a channel.
 * A channel groups related notifications so users can control them
 * individually in Settings → App notifications.
 *
 * We create 4 channels:
 *   daily_reminder_channel  — daily workout reminders
 *   rest_day_channel        — "rest day tomorrow" notice
 *   weekly_plan_channel     — Monday plan ready (reuses WeeklyReminderReceiver's)
 *   streak_channel          — streak protection reminders
 *
 * All channels are created at once in ensureChannelsExist().
 * Creating an existing channel is a no-op — safe to call repeatedly.
 */
public class NotificationHelper {

    // ── Channel IDs ───────────────────────────────────────────
    public static final String CHANNEL_DAILY   = "daily_reminder_channel";
    public static final String CHANNEL_REST    = "rest_day_channel";
    public static final String CHANNEL_WEEKLY  = "weekly_plan_channel";  // matches WeeklyReminderReceiver
    public static final String CHANNEL_STREAK  = "streak_channel";

    // ── Notification IDs ──────────────────────────────────────
    // Each type needs a unique ID so they show as separate notifications
    // and don't accidentally cancel each other.
    public static final int ID_DAILY   = 1001;
    public static final int ID_REST    = 1002;
    public static final int ID_WEEKLY  = 1003;  // matches WeeklyReminderReceiver
    public static final int ID_STREAK  = 1004;

    /*
     * Creates all notification channels.
     * Call this early (e.g. in MainActivity) so channels exist before
     * any notification is posted. Only executes on API 26+.
     */
    public static void ensureChannelsExist(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        // Daily reminder — high importance (heads-up banner)
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_DAILY,
                "Daily Workout Reminders",
                NotificationManager.IMPORTANCE_HIGH));

        // Rest day — default importance (no banner, just tray)
        NotificationChannel restChannel = new NotificationChannel(
                CHANNEL_REST,
                "Rest Day Notices",
                NotificationManager.IMPORTANCE_DEFAULT);
        restChannel.setDescription("Evening reminder when tomorrow is a rest day.");
        manager.createNotificationChannel(restChannel);

        // Weekly plan — high importance
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_WEEKLY,
                "Weekly Plan Reminders",
                NotificationManager.IMPORTANCE_HIGH));

        // Streak — default importance
        NotificationChannel streakChannel = new NotificationChannel(
                CHANNEL_STREAK,
                "Streak Reminders",
                NotificationManager.IMPORTANCE_DEFAULT);
        streakChannel.setDescription("Reminds you when your streak is at risk.");
        manager.createNotificationChannel(streakChannel);
    }

    /*
     * Posts a notification that opens MainActivity when tapped.
     *
     * PARAMETERS:
     *   context     — application context
     *   notifId     — unique notification ID (use constants above)
     *   channelId   — which channel (use constants above)
     *   title       — notification title line
     *   body        — notification body text
     *   bigText     — expanded text when notification is pulled down (nullable)
     *
     * HOW PendingIntent WORKS HERE:
     * We create a PendingIntent that opens MainActivity.
     * MainActivity then reads the session and routes correctly —
     * if logged in → weekly check → home; if not → auth screen.
     * We don't need separate intents per notification type.
     */
    public static void postNotification(Context context,
                                        int notifId,
                                        String channelId,
                                        String title,
                                        String body,
                                        String bigText) {

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        // Build the tap intent
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notifId, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Add expanded text if provided
        if (bigText != null && !bigText.isEmpty()) {
            builder.setStyle(
                    new NotificationCompat.BigTextStyle().bigText(bigText));
        }

        manager.notify(notifId, builder.build());
    }
}