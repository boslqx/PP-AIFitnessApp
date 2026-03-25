package com.example.aifitnessapp.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.aifitnessapp.MainActivity;
import com.example.aifitnessapp.R;

/*
 * WeeklyReminderReceiver
 *
 * A BroadcastReceiver that fires every Monday at the scheduled time.
 * Its only job: post a notification telling the user their new week is ready.
 *
 * LIFECYCLE:
 * BroadcastReceivers are NOT persistent — they're instantiated to handle
 * one broadcast, then destroyed. You cannot do long async work here.
 * Posting a notification is fast and synchronous, so it's safe.
 *
 * NOTIFICATION CHANNEL:
 * Android 8.0+ (API 26+) requires notifications to belong to a channel.
 * A channel is a category the user can configure in system settings
 * (e.g. mute all notifications from this channel).
 * We create the channel here if it doesn't exist yet.
 * Creating a channel that already exists is a no-op — safe to call repeatedly.
 *
 * PENDING INTENT:
 * When the user taps the notification, Android needs to know which
 * Activity to open. A PendingIntent is a pre-packaged instruction:
 * "when this notification is tapped, launch MainActivity."
 * "Pending" means the intent is held by the system and executed later
 * on the app's behalf — even if the app isn't running.
 */
public class WeeklyReminderReceiver extends BroadcastReceiver {

    // Channel ID — a stable string identifier for this notification category.
    // Users see this in Android Settings → App notifications.
    public static final String CHANNEL_ID   = "weekly_plan_channel";
    public static final String CHANNEL_NAME = "Weekly Plan Reminders";

    // Notification ID — must be unique within your app.
    // Using the same ID for the same type of notification means
    // a new notification replaces the old one (no duplicates).
    public static final int NOTIFICATION_ID = 1001;

    // Intent action — identifies this specific alarm to the receiver.
    // Useful if you later add multiple alarm types.
    public static final String ACTION_WEEKLY_REMINDER =
            "com.example.aifitnessapp.WEEKLY_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Step 1: Ensure the notification channel exists
        createNotificationChannel(context);

        // Step 2: Build the intent that opens MainActivity when tapped
        Intent openApp = new Intent(context, MainActivity.class);
        // FLAG_ACTIVITY_CLEAR_TOP: if MainActivity is already running,
        // bring it to the front instead of creating a second copy.
        openApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        /*
         * PendingIntent.FLAG_UPDATE_CURRENT:
         * If a PendingIntent with this request code already exists,
         * update it with the new intent's extras. Prevents stale intents.
         *
         * FLAG_IMMUTABLE (required API 31+):
         * The system cannot modify this PendingIntent. Safer and required
         * for alarms on modern Android versions.
         */
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Step 3: Build and post the notification
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("🗓 New week, new plan!")
                        .setContentText(
                                "Your weekly plan is ready to review. "
                                        + "Keep last week or start fresh?")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(
                                        "Your weekly plan is ready to review. "
                                                + "Open the app to keep last week's schedule "
                                                + "or let the AI generate a new plan based "
                                                + "on your progress."))
                        // AUTO_CANCEL: notification disappears when tapped
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        // PRIORITY_HIGH: shows as a heads-up notification
                        // (banner at the top of the screen)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    /*
     * Creates the notification channel.
     * Only needed on Android 8.0+ (API 26+).
     * The Build.VERSION check ensures backward compatibility with older devices.
     */
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    // IMPORTANCE_HIGH = shows heads-up notification banner
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(
                    "Reminds you when your weekly workout plan is ready.");

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}