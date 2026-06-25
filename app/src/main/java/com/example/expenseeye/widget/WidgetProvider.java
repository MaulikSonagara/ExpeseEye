package com.example.expenseeye.widget;

import android.net.Uri;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.example.expenseeye.MainActivity;
import com.example.expenseeye.QuickAddExpenseActivity;
import com.example.expenseeye.QuickAddChecklistActivity;
import com.example.expenseeye.R;
import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.theme.ThemeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Calendar;

public class WidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TOGGLE_CHECKLIST_ITEM = "com.example.expenseeye.widget.ACTION_TOGGLE_CHECKLIST_ITEM";
    public static final String EXTRA_CHECKLIST_ITEM_ID = "extra_checklist_item_id";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final PendingResult pendingResult = goAsync();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidgetSync(context, appWidgetManager, appWidgetId);
                }
            } finally {
                pendingResult.finish();
            }
        });
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        final PendingResult pendingResult = goAsync();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                updateAppWidgetSync(context, appWidgetManager, appWidgetId);
            } finally {
                pendingResult.finish();
            }
        });
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TOGGLE_CHECKLIST_ITEM.equals(intent.getAction())) {
            long itemId = intent.getLongExtra(EXTRA_CHECKLIST_ITEM_ID, -1);
            if (itemId != -1) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getDatabase(context);
                    com.example.expenseeye.models.ChecklistItem item = db.checklistItemDao().getItemByIdSync(itemId);
                    if (item != null) {
                        boolean willBeCompleted = !item.isCompleted();
                        item.setCompleted(willBeCompleted);
                        db.checklistItemDao().update(item);
                        updateAllWidgets(context);

                        if (willBeCompleted) {
                            Intent addExpenseIntent = new Intent(context, QuickAddExpenseActivity.class);
                            addExpenseIntent.putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, item.getCategory());
                            addExpenseIntent.putExtra("extra_title", item.getTitle());
                            addExpenseIntent.putExtra("extra_description", item.getQuantity() != null && !item.getQuantity().isEmpty() ? "Qty: " + item.getQuantity() : "");
                            addExpenseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            context.startActivity(addExpenseIntent);
                        }
                    }
                });
            }
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(new android.content.ComponentName(context, WidgetProvider.class));

        AppDatabase.databaseWriteExecutor.execute(() -> {
            for (int id : ids) {
                updateAppWidgetSync(context, appWidgetManager, id);
            }
            // Notify list views to reload new data from Room
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.lv_widget_checklist);
        });
    }

    static void updateAppWidgetSync(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

        // Determine Layout based on size
        int layoutId = getLayoutForSize(minWidth, minHeight);
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        // Fetch Data and Update
        AppDatabase db = AppDatabase.getDatabase(context);
        
        // Optimize: Fetch only last 31 days for widget
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -31);
        long thirtyDaysAgo = cal.getTimeInMillis();
        
        List<Expense> expenses = db.expenseDao().getExpensesInRangeSync(thirtyDaysAgo, System.currentTimeMillis());
        List<com.example.expenseeye.models.ChecklistItem> checklistItems = db.checklistItemDao().getAllChecklistItemsSync();

        WidgetData data = calculateWidgetData(expenses);

        views.setTextViewText(R.id.tv_widget_amount, String.format(Locale.getDefault(), "₹%.0f", data.todayTotal));

        // Size-dependent views
        if (hasView(layoutId, R.id.tv_widget_amount_week)) {
            views.setTextViewText(R.id.tv_widget_amount_week, String.format(Locale.getDefault(), "₹%.0f", data.weekTotal));
        }
        if (hasView(layoutId, R.id.tv_widget_amount_month)) {
            views.setTextViewText(R.id.tv_widget_amount_month, String.format(Locale.getDefault(), "₹%.0f", data.monthTotal));
        }
        if (hasView(layoutId, R.id.iv_widget_pie)) {
            int primary = ThemeManager.getColor(context, ThemeManager.ThemeColor.PRIMARY);
            int textSecondary = ThemeManager.getColor(context, ThemeManager.ThemeColor.TEXT_SECONDARY);
            views.setImageViewBitmap(R.id.iv_widget_pie, createDetailedPieChart(context, expenses, primary, textSecondary));
        }

        // Payment method stats
        if (hasView(layoutId, R.id.tv_widget_pay_cash)) {
            views.setTextViewText(R.id.tv_widget_pay_cash, String.format(Locale.getDefault(), "Cash: ₹%.0f", data.cashTotal));
            views.setTextViewText(R.id.tv_widget_pay_upi, String.format(Locale.getDefault(), "UPI: ₹%.0f", data.upiTotal));
            views.setTextViewText(R.id.tv_widget_pay_card, String.format(Locale.getDefault(), "Card: ₹%.0f", data.cardTotal));
        }

        // Checklist Setup (ListView Adapter and template PendingIntent)
        if (hasView(layoutId, R.id.lv_widget_checklist)) {
            Intent serviceIntent = new Intent(context, WidgetChecklistService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
            views.setRemoteAdapter(R.id.lv_widget_checklist, serviceIntent);

            // Setup broadcast intent template
            Intent clickIntent = new Intent(context, WidgetProvider.class);
            clickIntent.setAction(ACTION_TOGGLE_CHECKLIST_ITEM);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent clickPI = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.lv_widget_checklist, clickPI);

            // Show/hide empty state
            boolean hasPending = false;
            for (com.example.expenseeye.models.ChecklistItem ci : checklistItems) {
                if (!ci.isCompleted()) {
                    hasPending = true;
                    break;
                }
            }
            if (hasPending) {
                views.setViewVisibility(R.id.tv_widget_checklist_empty, android.view.View.GONE);
                views.setViewVisibility(R.id.lv_widget_checklist, android.view.View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.tv_widget_checklist_empty, android.view.View.VISIBLE);
                views.setViewVisibility(R.id.lv_widget_checklist, android.view.View.GONE);
            }
        }

        // Click Intents for standard buttons
        setupClickIntents(context, views);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static int getLayoutForSize(int width, int height) {
        int cols = getCells(width);
        int rows = getCells(height);

        if (cols >= 4) {
            if (rows >= 4) return R.layout.widget_4x4;
            if (rows >= 3) return R.layout.widget_4x3;
            if (rows >= 2) return R.layout.widget_4x2;
            return R.layout.widget_4x1;
        } else if (cols >= 3) {
            if (rows >= 3) return R.layout.widget_3x3;
            if (rows >= 2) return R.layout.widget_3x2;
            return R.layout.widget_3x1;
        } else {
            if (rows >= 4) return R.layout.widget_2x4;
            if (rows >= 3) return R.layout.widget_2x3;
            if (rows >= 2) return R.layout.widget_2x2;
            return R.layout.widget_2x1;
        }
    }

    private static int getCells(int size) {
        return (size + 30) / 70;
    }



    private static void setupClickIntents(Context context, RemoteViews views) {
        // Main Click (Open Dashboard)
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPI = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.layout_widget_main_click, mainPI);

        // Add Click (Open Quick Add)
        Intent addIntent = new Intent(context, QuickAddExpenseActivity.class);
        PendingIntent addPI = PendingIntent.getActivity(context, 1, addIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_widget_add, addPI);

        // Add Checklist Click (Open Quick Add Checklist)
        Intent addChecklistIntent = new Intent(context, QuickAddChecklistActivity.class);
        PendingIntent addChecklistPI = PendingIntent.getActivity(context, 2, addChecklistIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_widget_add_checklist, addChecklistPI);

        // Pie click if exists
        Intent pieIntent = new Intent(context, MainActivity.class);
        PendingIntent piePI = PendingIntent.getActivity(context, 3, pieIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.layout_widget_pie_click, piePI);
    }

    private static boolean hasView(int layoutId, int viewId) {
        if (viewId == R.id.tv_widget_amount_week) {
            return layoutId != R.layout.widget_2x1;
        }
        if (viewId == R.id.tv_widget_amount_month) {
            return layoutId == R.layout.widget_4x1 || layoutId == R.layout.widget_4x2 ||
                    layoutId == R.layout.widget_4x3 || layoutId == R.layout.widget_4x4 ||
                    layoutId == R.layout.widget_3x3;
        }
        if (viewId == R.id.iv_widget_pie) {
            return layoutId == R.layout.widget_4x3 || layoutId == R.layout.widget_4x4 ||
                    layoutId == R.layout.widget_3x3;
        }
        if (viewId == R.id.tv_widget_pay_cash || viewId == R.id.tv_widget_pay_upi || viewId == R.id.tv_widget_pay_card) {
            return layoutId == R.layout.widget_4x3 || layoutId == R.layout.widget_4x4;
        }

        if (viewId == R.id.lv_widget_checklist) {
            return layoutId == R.layout.widget_2x3 || layoutId == R.layout.widget_2x4 ||
                    layoutId == R.layout.widget_3x3 || layoutId == R.layout.widget_4x3 ||
                    layoutId == R.layout.widget_4x4;
        }
        return true;
    }

    private static WidgetData calculateWidgetData(List<Expense> expenses) {
        WidgetData data = new WidgetData();
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        long weekStart = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, 1);
        long monthStart = cal.getTimeInMillis();

        for (Expense e : expenses) {
            long t = e.getTimestamp();
            double amt = e.getAmount();

            if (t >= monthStart) data.monthTotal += amt;
            if (t >= weekStart) data.weekTotal += amt;
            if (t >= todayStart) data.todayTotal += amt;

            String pmName = e.getPaymentMethodName();
            if (pmName != null) {
                if (pmName.equalsIgnoreCase("Cash")) {
                    data.cashTotal += amt;
                } else if (pmName.equalsIgnoreCase("UPI")) {
                    data.upiTotal += amt;
                } else if (pmName.toLowerCase().contains("card")) {
                    data.cardTotal += amt;
                }
            }
        }
        return data;
    }

    private static Bitmap createDetailedPieChart(Context context, List<Expense> expenses, int primaryColor, int textSecondaryColor) {
        Bitmap bitmap = Bitmap.createBitmap(640, 240, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Group month's expenses by category
        Map<String, Double> catTotals = new HashMap<>();
        double totalSpend = 0;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monthStart = cal.getTimeInMillis();

        for (Expense e : expenses) {
            if (e.getTimestamp() >= monthStart) {
                String cat = e.getCategoryName();
                if (cat == null) cat = "Other";
                double amt = e.getAmount();
                catTotals.put(cat, catTotals.getOrDefault(cat, 0.0) + amt);
                totalSpend += amt;
            }
        }

        if (totalSpend == 0) {
            // Draw empty state donut (scaled)
            paint.setColor(android.graphics.Color.parseColor("#334155"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(24);
            canvas.drawCircle(120, 120, 72, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(android.graphics.Color.parseColor("#94A3B8"));
            paint.setTextSize(32f);
            paint.setFakeBoldText(true);
            canvas.drawText("No expenses", 260, 110, paint);
            canvas.drawText("recorded this month", 260, 155, paint);
            return bitmap;
        }

        // Sort by amount descending
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(catTotals.entrySet());
        Collections.sort(sorted, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Prepare slices
        List<ChartSlice> slices = new ArrayList<>();
        double otherAmt = 0;
        int[] colorPalette = {
                android.graphics.Color.parseColor("#6C7CFF"), // Primary blue
                android.graphics.Color.parseColor("#5DD6C0"), // Teal
                android.graphics.Color.parseColor("#FB923C"), // Orange
                android.graphics.Color.parseColor("#A78BFA"), // Purple
                android.graphics.Color.parseColor("#F87171")  // Red/Rose
        };

        for (int i = 0; i < sorted.size(); i++) {
            if (i < 3) {
                slices.add(new ChartSlice(sorted.get(i).getKey(), sorted.get(i).getValue(), colorPalette[i % colorPalette.length]));
            } else {
                otherAmt += sorted.get(i).getValue();
            }
        }

        if (otherAmt > 0) {
            slices.add(new ChartSlice("Other", otherAmt, colorPalette[3 % colorPalette.length]));
        }

        // Draw donut chart (scaled)
        float startAngle = -90f;
        RectF oval = new RectF(48, 48, 192, 192);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(24);

        for (ChartSlice slice : slices) {
            float sweepAngle = (float) (slice.amount / totalSpend * 360f);
            paint.setColor(slice.color);
            canvas.drawArc(oval, startAngle, sweepAngle, false, paint);
            startAngle += sweepAngle;
        }

        // Draw Legend next to donut (scaled and bold)
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(28f);
        paint.setFakeBoldText(true);

        for (int i = 0; i < slices.size() && i < 3; i++) {
            ChartSlice slice = slices.get(i);
            double percent = (slice.amount / totalSpend) * 100;

            // Draw color dot
            paint.setColor(slice.color);
            canvas.drawCircle(270, 74 + i * 52, 10, paint);

            // Draw category info text
            paint.setColor(android.graphics.Color.parseColor("#F8FAFC"));
            String label = String.format(Locale.getDefault(), "%s: %.0f%% (₹%.0f)", slice.name, percent, slice.amount);
            canvas.drawText(label, 295, 84 + i * 52, paint);
        }

        return bitmap;
    }

    private static class ChartSlice {
        String name;
        double amount;
        int color;

        ChartSlice(String name, double amount, int color) {
            this.name = name;
            this.amount = amount;
            this.color = color;
        }
    }

    private static class WidgetData {
        double todayTotal = 0, weekTotal = 0, monthTotal = 0;
        double cashTotal = 0, upiTotal = 0, cardTotal = 0;
    }
}
