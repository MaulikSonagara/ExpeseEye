package com.example.expenseeye.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.example.expenseeye.MainActivity;
import com.example.expenseeye.QuickAddExpenseActivity;
import com.example.expenseeye.R;
import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.Expense;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WidgetProvider extends AppWidgetProvider {

    private static final String PREF_NAME = "WidgetPrefs";
    private static final String KEY_CACHE_TODAY = "cache_today";
    private static final String KEY_CACHE_WEEK = "cache_week";
    private static final String KEY_CACHE_MONTH = "cache_month";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateAppWidget(context, appWidgetManager, appWidgetId);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    private static int getSpanX(int width) {
        if (width < 180) return 2;
        if (width < 250) return 3;
        return 4;
    }

    private static int getSpanY(int height) {
        if (height < 110) return 1;
        if (height < 180) return 2;
        if (height < 250) return 3;
        return 4;
    }

    private static int getLayoutResId(int cols, int rows) {
        if (rows == 1) {
            if (cols == 2) return R.layout.widget_2x1;
            if (cols == 3) return R.layout.widget_3x1;
            return R.layout.widget_4x1;
        } else if (rows == 2) {
            if (cols == 2) return R.layout.widget_2x2;
            if (cols == 3) return R.layout.widget_3x2;
            return R.layout.widget_4x2;
        } else if (rows == 3) {
            if (cols == 2) return R.layout.widget_2x3;
            if (cols == 3) return R.layout.widget_3x3;
            return R.layout.widget_4x3;
        } else { // rows >= 4
            if (cols == 4) return R.layout.widget_4x4;
            if (cols == 2) return R.layout.widget_2x3;
            return R.layout.widget_3x3;
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Query size
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40);

        int cols = getSpanX(minWidth);
        int rows = getSpanY(minHeight);

        int layoutId = getLayoutResId(cols, rows);
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        // Pre-render immediately using cached values to avoid lag/flicker
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        float cachedToday = prefs.getFloat(KEY_CACHE_TODAY, 0.0f);
        float cachedWeek = prefs.getFloat(KEY_CACHE_WEEK, 0.0f);
        float cachedMonth = prefs.getFloat(KEY_CACHE_MONTH, 0.0f);

        views.setTextViewText(R.id.tv_widget_amount, String.format(Locale.getDefault(), "₹%.2f", cachedToday));
        
        if (layoutId == R.layout.widget_4x1 || layoutId == R.layout.widget_2x2 || layoutId == R.layout.widget_3x2 ||
            layoutId == R.layout.widget_4x2 || layoutId == R.layout.widget_2x3 || layoutId == R.layout.widget_3x3 ||
            layoutId == R.layout.widget_4x3 || layoutId == R.layout.widget_4x4) {
            views.setTextViewText(R.id.tv_widget_amount_week, String.format(Locale.getDefault(), "₹%.2f", cachedWeek));
        }

        if (layoutId == R.layout.widget_4x2 || layoutId == R.layout.widget_2x3 || layoutId == R.layout.widget_3x3 ||
            layoutId == R.layout.widget_4x3 || layoutId == R.layout.widget_4x4) {
            views.setTextViewText(R.id.tv_widget_amount_month, String.format(Locale.getDefault(), "₹%.2f", cachedMonth));
        }

        // Setup main and add click routes immediately
        Intent intentMain = new Intent(context, MainActivity.class);
        intentMain.putExtra("navigate_to", R.id.dashboardFragment);
        intentMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent piMain = PendingIntent.getActivity(
                context, 301, intentMain, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.layout_widget_main_click, piMain);

        Intent intentAdd = new Intent(context, QuickAddExpenseActivity.class);
        intentAdd.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent piAdd = PendingIntent.getActivity(
                context, 104, intentAdd, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_widget_add, piAdd);

        if (layoutId == R.layout.widget_3x3 || layoutId == R.layout.widget_4x3 || layoutId == R.layout.widget_4x4) {
            Intent intentPie = new Intent(context, MainActivity.class);
            intentPie.putExtra("navigate_to", R.id.reportsFragment);
            intentPie.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent piPie = PendingIntent.getActivity(
                    context, 302, intentPie, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.layout_widget_pie_click, piPie);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);

        // Async heavy DB calculation
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long todayStart = cal.getTimeInMillis();

            Calendar calEnd = Calendar.getInstance();
            calEnd.set(Calendar.HOUR_OF_DAY, 23);
            calEnd.set(Calendar.MINUTE, 59);
            calEnd.set(Calendar.SECOND, 59);
            calEnd.set(Calendar.MILLISECOND, 999);
            long todayEnd = calEnd.getTimeInMillis();

            Calendar calWeek = Calendar.getInstance();
            calWeek.set(Calendar.DAY_OF_WEEK, calWeek.getFirstDayOfWeek());
            calWeek.set(Calendar.HOUR_OF_DAY, 0);
            calWeek.set(Calendar.MINUTE, 0);
            calWeek.set(Calendar.SECOND, 0);
            calWeek.set(Calendar.MILLISECOND, 0);
            long weekStart = calWeek.getTimeInMillis();

            Calendar calMonth = Calendar.getInstance();
            calMonth.set(Calendar.DAY_OF_MONTH, 1);
            calMonth.set(Calendar.HOUR_OF_DAY, 0);
            calMonth.set(Calendar.MINUTE, 0);
            calMonth.set(Calendar.SECOND, 0);
            calMonth.set(Calendar.MILLISECOND, 0);
            long monthStart = calMonth.getTimeInMillis();

            double totalToday = db.expenseDao().getTotalSpendingInRange(todayStart, todayEnd);
            double totalWeek = db.expenseDao().getTotalSpendingInRange(weekStart, todayEnd);
            double totalMonth = db.expenseDao().getTotalSpendingInRange(monthStart, todayEnd);

            // Update cache values
            prefs.edit()
                    .putFloat(KEY_CACHE_TODAY, (float) totalToday)
                    .putFloat(KEY_CACHE_WEEK, (float) totalWeek)
                    .putFloat(KEY_CACHE_MONTH, (float) totalMonth)
                    .apply();

            // Populate fresh values
            views.setTextViewText(R.id.tv_widget_amount, String.format(Locale.getDefault(), "₹%.2f", totalToday));
            
            if (layoutId == R.layout.widget_4x1 || layoutId == R.layout.widget_2x2 || layoutId == R.layout.widget_3x2 ||
                layoutId == R.layout.widget_4x2 || layoutId == R.layout.widget_2x3 || layoutId == R.layout.widget_3x3 ||
                layoutId == R.layout.widget_4x3 || layoutId == R.layout.widget_4x4) {
                views.setTextViewText(R.id.tv_widget_amount_week, String.format(Locale.getDefault(), "₹%.2f", totalWeek));
            }

            if (layoutId == R.layout.widget_4x2 || layoutId == R.layout.widget_2x3 || layoutId == R.layout.widget_3x3 ||
                layoutId == R.layout.widget_4x3 || layoutId == R.layout.widget_4x4) {
                views.setTextViewText(R.id.tv_widget_amount_month, String.format(Locale.getDefault(), "₹%.2f", totalMonth));
            }

            // Draw Category distribution arc chart (for 3x3, 4x3, and 4x4)
            if (layoutId == R.layout.widget_3x3 || layoutId == R.layout.widget_4x3 || layoutId == R.layout.widget_4x4) {
                List<Expense> expenses = db.expenseDao().getAllExpensesSync();
                Map<String, Double> categoryTotals = new HashMap<>();
                Map<String, Integer> categoryColors = new HashMap<>();

                List<Category> cats = db.categoryDao().getAllCategoriesSync();
                for (Category c : cats) {
                    categoryColors.put(c.getName(), c.getColor());
                }

                for (Expense e : expenses) {
                    if (e.getTimestamp() >= monthStart && e.getTimestamp() <= todayEnd) {
                        String cat = e.getCategoryName();
                        double amount = e.getAmount();
                        double currentTotal = categoryTotals.containsKey(cat) ? categoryTotals.get(cat) : 0;
                        categoryTotals.put(cat, currentTotal + amount);
                    }
                }

                Bitmap pieChart = drawDonutChart(categoryTotals, categoryColors, totalMonth, 160, 160);
                views.setImageViewBitmap(R.id.iv_widget_pie, pieChart);
            }

            // Bind Quick actions
            List<String> actions = WidgetPreferenceManager.getQuickActions(context);

            if (layoutId == R.layout.widget_3x1 || layoutId == R.layout.widget_4x1 ||
                layoutId == R.layout.widget_3x2 || layoutId == R.layout.widget_3x3) {
                bindQuickActions(context, views, actions, db, R.id.btn_widget_action_1);
            } else if (layoutId == R.layout.widget_4x2 || layoutId == R.layout.widget_4x3) {
                bindQuickActions(context, views, actions, db, R.id.btn_widget_action_1, R.id.btn_widget_action_2);
            } else if (layoutId == R.layout.widget_4x4) {
                bindQuickActions(context, views, actions, db, R.id.btn_widget_action_1, R.id.btn_widget_action_2, R.id.btn_widget_action_3);
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        });
    }

    private static void bindQuickActions(Context context, RemoteViews views, List<String> actions, AppDatabase db, int... btnIds) {
        int[] ivIds = new int[btnIds.length];
        int[] tvIds = new int[btnIds.length];

        for (int i = 0; i < btnIds.length; i++) {
            int btnId = btnIds[i];
            if (btnId == R.id.btn_widget_action_1) {
                ivIds[i] = R.id.iv_widget_action_1;
                tvIds[i] = R.id.tv_widget_action_1;
            } else if (btnId == R.id.btn_widget_action_2) {
                ivIds[i] = R.id.iv_widget_action_2;
                tvIds[i] = R.id.tv_widget_action_2;
            } else if (btnId == R.id.btn_widget_action_3) {
                ivIds[i] = R.id.iv_widget_action_3;
                tvIds[i] = R.id.tv_widget_action_3;
            }
        }

        for (int i = 0; i < btnIds.length; i++) {
            if (i < actions.size()) {
                String name = actions.get(i);
                Category category = db.categoryDao().getByName(name);
                views.setViewVisibility(btnIds[i], View.VISIBLE);
                views.setTextViewText(tvIds[i], " " + name);

                int resId = context.getResources().getIdentifier(
                        category != null ? category.getIconName() : "ic_other",
                        "drawable",
                        context.getPackageName()
                );
                if (resId != 0) {
                    views.setImageViewResource(ivIds[i], resId);
                } else {
                    views.setImageViewResource(ivIds[i], R.drawable.ic_other);
                }

                if (category != null) {
                    views.setInt(ivIds[i], "setColorFilter", category.getColor());
                } else {
                    views.setInt(ivIds[i], "setColorFilter", Color.GRAY);
                }

                Intent intentAction = new Intent(context, QuickAddExpenseActivity.class);
                intentAction.putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, name);
                intentAction.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent piAction = PendingIntent.getActivity(
                        context, 200 + i, intentAction, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(btnIds[i], piAction);
            } else {
                views.setViewVisibility(btnIds[i], View.GONE);
            }
        }
    }

    private static Bitmap drawDonutChart(Map<String, Double> categoryTotals, Map<String, Integer> categoryColors, double total, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);

        if (categoryTotals == null || categoryTotals.isEmpty() || total <= 0) {
            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#334155"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(16f);
            paint.setAntiAlias(true);
            canvas.drawCircle(width / 2f, height / 2f, Math.min(width, height) / 2f - 12f, paint);
            return bitmap;
        }

        float strokeWidth = 16f;
        RectF rect = new RectF(
                strokeWidth, strokeWidth,
                width - strokeWidth, height - strokeWidth
        );

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);

        float startAngle = -90f;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            double val = entry.getValue();
            float sweepAngle = (float) (val / total * 360.0);

            int color = categoryColors.containsKey(entry.getKey()) ? categoryColors.get(entry.getKey()) : Color.GRAY;
            paint.setColor(color);

            canvas.drawArc(rect, startAngle, sweepAngle, false, paint);
            startAngle += sweepAngle;
        }

        return bitmap;
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, WidgetProvider.class));
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            Intent intent = new Intent(context, WidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            context.sendBroadcast(intent);
        }
    }
}
