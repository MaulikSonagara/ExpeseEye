package com.example.expenseeye.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver to catch scheduled alarms and trigger notifications.
 */
public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm Received!");
        // Trigger the ReminderEngine to check for any due expenses and log them
        ReminderEngine.checkAndProcessReminders(context);

        // Extract data passed from AlarmScheduler
        int id = intent.getIntExtra("id", 1001);
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String channelId = intent.getStringExtra("channel_id");

        // Defaults
        if (title == null) title = "ExpenseEye Reminder";
        if (message == null) message = "Don't forget to track your expenses today!";
        if (channelId == null) channelId = NotificationHelper.CHANNEL_REMINDERS;

        Log.d(TAG, "Showing notification: ID=" + id + ", Title=" + title);

        // Trigger the notification
        NotificationHelper.showNotification(context, id, title, message, channelId);
    }
}
