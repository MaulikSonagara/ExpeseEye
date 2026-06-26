package com.example.expenseeye.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.expenseeye.MainActivity;
import com.example.expenseeye.R;

/**
 * Helper class to manage notification channels and showing notifications.
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    public static final String CHANNEL_REMINDERS = "expense_eye_reminders";
    public static final String CHANNEL_ALERTS = "expense_eye_alerts";

    /**
     * Initializes all notification channels for the app.
     * Should be called on app startup (e.g., MainActivity onCreate).
     */
    public static void initNotificationChannels(Context context) {
        Log.d(TAG, "Initializing notification channels...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Reminders Channel
            createChannel(context, 
                    CHANNEL_REMINDERS, 
                    "Reminders", 
                    "Daily and weekly expense tracking reminders", 
                    NotificationManager.IMPORTANCE_DEFAULT);

            // Alerts Channel
            createChannel(context, 
                    CHANNEL_ALERTS, 
                    "Alerts", 
                    "Budget exceeded and pending bill alerts", 
                    NotificationManager.IMPORTANCE_HIGH);
        }
    }

    private static void createChannel(Context context, String id, String name, String desc, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            channel.setDescription(desc);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Shows a local notification.
     * 
     * @param context Context
     * @param id Unique notification ID
     * @param title Title of the notification
     * @param message Body message
     */
    public static void showNotification(Context context, int id, String title, String message) {
        showNotification(context, id, title, message, CHANNEL_REMINDERS);
    }

    /**
     * Shows a local notification with a specific channel.
     * 
     * @param context Context
     * @param id Unique notification ID
     * @param title Title of the notification
     * @param message Body message
     * @param channelId The channel ID to use
     */
    public static void showNotification(Context context, int id, String title, String message, String channelId) {
        Log.d(TAG, "Attempting to show notification: ID=" + id + ", Title=" + title + ", Channel=" + channelId);
        // Tap action to open MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // PendingIntent flags for API compatibility (FLAG_IMMUTABLE for 31+)
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notifications) // Use app's notification icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(id, builder.build());
        } catch (SecurityException e) {
            // Permission not granted on Android 13+
            e.printStackTrace();
        }
    }
}
