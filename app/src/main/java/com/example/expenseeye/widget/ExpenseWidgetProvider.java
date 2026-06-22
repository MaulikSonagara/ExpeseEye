package com.example.expenseeye.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.expenseeye.MainActivity;
import com.example.expenseeye.QuickAddExpenseActivity;
import com.example.expenseeye.R;
import com.example.expenseeye.SplashActivity;
import com.example.expenseeye.database.AppDatabase;

import java.util.Calendar;
import java.util.Locale;

public class ExpenseWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Calculate today's bounds (12:00 AM to 11:59:59 PM)
            Calendar calStart = Calendar.getInstance();
            calStart.set(Calendar.HOUR_OF_DAY, 0);
            calStart.set(Calendar.MINUTE, 0);
            calStart.set(Calendar.SECOND, 0);
            calStart.set(Calendar.MILLISECOND, 0);
            long startTs = calStart.getTimeInMillis();

            Calendar calEnd = Calendar.getInstance();
            calEnd.set(Calendar.HOUR_OF_DAY, 23);
            calEnd.set(Calendar.MINUTE, 59);
            calEnd.set(Calendar.SECOND, 59);
            calEnd.set(Calendar.MILLISECOND, 999);
            long endTs = calEnd.getTimeInMillis();

            // Fetch total spending for today
            AppDatabase db = AppDatabase.getDatabase(context);
            double totalToday = db.expenseDao().getTotalSpendingInRange(startTs, endTs);

            // Construct RemoteViews
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_expense);

            // Format amount
            String formattedTotal = String.format(Locale.getDefault(), "₹%.2f", totalToday);
            views.setTextViewText(R.id.tv_widget_amount, formattedTotal);

            // Set up Pending Intents for category logging buttons
            // 1. Groceries Button -> Opens dialog pre-selected for Groceries
            Intent intentGroceries = new Intent(context, QuickAddExpenseActivity.class);
            intentGroceries.putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, "Groceries");
            intentGroceries.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent piGroceries = PendingIntent.getActivity(
                    context, 101, intentGroceries, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.btn_widget_groceries, piGroceries);

            // 2. Transport Button -> Opens dialog pre-selected for Transport
            Intent intentTransport = new Intent(context, QuickAddExpenseActivity.class);
            intentTransport.putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, "Transport");
            intentTransport.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent piTransport = PendingIntent.getActivity(
                    context, 102, intentTransport, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.btn_widget_transport, piTransport);

            // 3. Shopping Button -> Opens dialog pre-selected for Shopping
            Intent intentShopping = new Intent(context, QuickAddExpenseActivity.class);
            intentShopping.putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, "Shopping");
            intentShopping.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent piShopping = PendingIntent.getActivity(
                    context, 103, intentShopping, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.btn_widget_shopping, piShopping);

            // 4. General Add (+) Button -> Opens blank Quick Log dialog
            Intent intentAdd = new Intent(context, QuickAddExpenseActivity.class);
            intentAdd.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent piAdd = PendingIntent.getActivity(
                    context, 104, intentAdd, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.btn_widget_add, piAdd);

            // 5. App Logo/Total branding area -> Opens App Main Page
            Intent intentApp = new Intent(context, SplashActivity.class);
            intentApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent piApp = PendingIntent.getActivity(
                    context, 105, intentApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.tv_widget_title, piApp);
            views.setOnClickPendingIntent(R.id.tv_widget_amount, piApp);
            views.setOnClickPendingIntent(R.id.tv_widget_label, piApp);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        });
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, ExpenseWidgetProvider.class));
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            Intent intent = new Intent(context, ExpenseWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            context.sendBroadcast(intent);
        }
    }
}
