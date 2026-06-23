package com.example.expenseeye.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.expenseeye.MainActivity;
import com.example.expenseeye.R;
import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.theme.ThemeManager;

import java.util.List;
import java.util.Locale;

public class WidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_2x1);

        // Apply Theme Colors to Widget (Partial support due to RemoteViews limitations)
        int textPrimary = ThemeManager.getColor(context, ThemeManager.ThemeColor.TEXT_PRIMARY);

        views.setTextColor(R.id.tv_widget_amount, textPrimary);

        // Fetch Data
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            List<Expense> expenses = db.expenseDao().getAllExpensesSync();
            double todayTotal = 0;
            long now = System.currentTimeMillis();
            for (Expense e : expenses) {
                if (isSameDay(e.getTimestamp(), now)) {
                    todayTotal += e.getAmount();
                }
            }

            views.setTextViewText(R.id.tv_widget_amount, String.format(Locale.getDefault(), "₹%.2f", todayTotal));

            // Intent to open app
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.layout_widget_main_click, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }).start();
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(new android.content.ComponentName(context, WidgetProvider.class));
        for (int id : ids) {
            updateAppWidget(context, appWidgetManager, id);
        }
    }

    private static boolean isSameDay(long t1, long t2) {
        return (t1 / 86400000) == (t2 / 86400000);
    }
}
