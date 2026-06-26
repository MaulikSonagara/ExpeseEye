package com.example.expenseeye.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.ReminderExpense;

import java.util.List;

/**
 * Reschedules all active alarms after a device reboot.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Device Boot Received! Action: " + intent.getAction());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule all active reminders
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(context);
                List<ReminderExpense> activeReminders = db.reminderExpenseDao().getActiveReminderExpensesSync();
                
                if (activeReminders != null) {
                    for (ReminderExpense re : activeReminders) {
                        if (re.isEnabled() && re.getNextDueTimestamp() > System.currentTimeMillis()) {
                            AlarmScheduler.scheduleOneTime(
                                    context,
                                    re.getId(),
                                    "Reminder: " + re.getTitle(),
                                    "Your scheduled expense is due.",
                                    re.getNextDueTimestamp()
                            );
                        }
                    }
                }
            });

            // Reschedule Daily Log Reminder
            com.example.expenseeye.theme.ThemePreferenceHelper themeHelper = new com.example.expenseeye.theme.ThemePreferenceHelper(context);
            if (themeHelper.isDailyReminderEnabled()) {
                String time = themeHelper.getDailyReminderTime();
                String[] parts = time.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                AlarmScheduler.scheduleDaily(
                        context,
                        9999,
                        "Daily Log Reminder",
                        "Time to log your expenses for today!",
                        hour,
                        minute
                );
            }
        }
    }
}
