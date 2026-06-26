package com.example.expenseeye.utils;

import android.content.Context;
import android.util.Log;

import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.ReminderExpense;
import com.example.expenseeye.widget.WidgetProvider;

import java.util.Calendar;
import java.util.List;

public class ReminderEngine {
    private static final String TAG = "ReminderEngine";

    public static void checkAndProcessReminders(Context context) {
        Log.d(TAG, "Checking and processing reminders...");
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            List<ReminderExpense> activeReminders = db.reminderExpenseDao().getActiveReminderExpensesSync();
            if (activeReminders == null || activeReminders.isEmpty()) {
                Log.d(TAG, "No active reminders found.");
                return;
            }

            Log.d(TAG, "Found " + activeReminders.size() + " active reminders.");

            long now = System.currentTimeMillis();
            boolean anyLogged = false;

            for (ReminderExpense re : activeReminders) {
                long currentDue = re.getNextDueTimestamp();
                if (currentDue <= 0 || now < currentDue) {
                    continue;
                }

                anyLogged = true;

                while (currentDue > 0 && now >= currentDue) {
                    // Create the Expense
                    Expense expense = new Expense(
                            re.getTitle(),
                            "Auto-logged from reminder",
                            re.getAmount(),
                            currentDue,
                            re.getCategoryId(),
                            re.getCategoryName(),
                            re.getPaymentMethodId(),
                            re.getPaymentMethodName(),
                            re.getType()
                    );
                    db.expenseDao().insert(expense);

                    // Show notification
                    NotificationHelper.showNotification(
                            context,
                            re.getId() + (int) (currentDue % 100000),
                            "Reminder: Expense Logged",
                            String.format("Automatically logged '%s' of ₹%.2f", re.getTitle(), re.getAmount())
                    );

                    // Calculate next due timestamp
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(currentDue);
                    switch (re.getFrequency().toUpperCase()) {
                        case "DAILY":
                            cal.add(Calendar.DAY_OF_YEAR, 1);
                            break;
                        case "WEEKLY":
                            cal.add(Calendar.WEEK_OF_YEAR, 1);
                            break;
                        case "MONTHLY":
                            cal.add(Calendar.MONTH, 1);
                            break;
                        default:
                            currentDue = 0; // stop loop for one-time
                            break;
                    }
                    if (currentDue != 0) {
                        currentDue = cal.getTimeInMillis();
                    }
                }

                // Update lastLoggedTimestamp and nextDueTimestamp
                re.setLastLoggedTimestamp(now);
                re.setNextDueTimestamp(currentDue > 0 ? currentDue : 0);
                
                // If it was one-time (currentDue <= 0), we disable it
                if (currentDue <= 0) {
                    re.setEnabled(false);
                } else {
                    // Schedule next alarm
                    AlarmScheduler.scheduleOneTime(
                            context,
                            re.getId(),
                            "Reminder: " + re.getTitle(),
                            "Your scheduled expense is due.",
                            currentDue
                    );
                }
                db.reminderExpenseDao().update(re);
            }

            if (anyLogged) {
                WidgetProvider.updateAllWidgets(context);
            }
        });
    }
}
