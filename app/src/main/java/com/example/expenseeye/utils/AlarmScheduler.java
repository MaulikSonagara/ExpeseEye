package com.example.expenseeye.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Utility class to schedule and cancel alarms for reminders.
 */
public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";

    /**
     * Checks if the app can schedule exact alarms.
     * On Android 12+, this requires user permission.
     */
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true; // Below Android 12, permission is not required
    }

    /**
     * Directs the user to the system settings to grant exact alarm permission.
     */
    public static void requestExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * Schedules a one-time reminder.
     * 
     * @param context Context
     * @param id Unique ID for the alarm
     * @param title Title for the notification
     * @param message Message for the notification
     * @param timeInMillis Exact time to trigger the alarm
     */
    public static void scheduleOneTime(Context context, int id, String title, String message, long timeInMillis) {
        Log.d(TAG, "Scheduling OneTime alarm: ID=" + id + ", Title=" + title + ", Time=" + timeInMillis);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = createIntent(context, id, title, message);
        PendingIntent pendingIntent = createPendingIntent(context, id, intent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } else {
                Log.w(TAG, "Exact alarm permission missing. Falling back to inexact alarm.");
                // Fallback: This might be delayed by the system, but won't crash
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }
    }

    /**
     * Schedules a daily repeating reminder at a specific time.
     * 
     * @param context Context
     * @param id Unique ID for the alarm
     * @param title Title for the notification
     * @param message Message for the notification
     * @param hour Hour of day (0-23)
     * @param minute Minute (0-59)
     */
    public static void scheduleDaily(Context context, int id, String title, String message, int hour, int minute) {
        Log.d(TAG, "Scheduling Daily alarm: ID=" + id + ", Time=" + hour + ":" + minute);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = createIntent(context, id, title, message);
        PendingIntent pendingIntent = createPendingIntent(context, id, intent);

        // Note: setRepeating is not exact on modern Android versions. 
        // For production, consider using WorkManager or rescheduling exact alarms individually.
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    /**
     * Schedules a weekly repeating reminder.
     * 
     * @param context Context
     * @param id Unique ID
     * @param title Title
     * @param message Message
     * @param dayOfWeek Calendar.SUNDAY, etc.
     * @param hour Hour
     * @param minute Minute
     */
    public static void scheduleWeekly(Context context, int id, String title, String message, int dayOfWeek, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }

        Intent intent = createIntent(context, id, title, message);
        PendingIntent pendingIntent = createPendingIntent(context, id, intent);

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
        );
    }

    /**
     * Cancels a scheduled alarm.
     * 
     * @param context Context
     * @param id Unique ID used when scheduling
     */
    public static void cancelAlarm(Context context, int id) {
        Log.d(TAG, "Cancelling alarm: ID=" + id);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = createPendingIntent(context, id, intent);
        alarmManager.cancel(pendingIntent);
    }

    private static Intent createIntent(Context context, int id, String title, String message) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("id", id);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        return intent;
    }

    private static PendingIntent createPendingIntent(Context context, int id, Intent intent) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, id, intent, flags);
    }
}
