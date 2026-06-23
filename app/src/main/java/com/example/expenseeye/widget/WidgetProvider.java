package com.example.expenseeye.widget;

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
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.example.expenseeye.MainActivity;
import com.example.expenseeye.QuickAddExpenseActivity;
import com.example.expenseeye.QuickAddChecklistActivity;
import com.example.expenseeye.R;
import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.theme.ThemeManager;

import java.util.List;
import java.util.Locale;
import java.util.Calendar;

public class WidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TOGGLE_CHECKLIST_ITEM = "com.example.expenseeye.widget.ACTION_TOGGLE_CHECKLIST_ITEM";
    public static final String EXTRA_CHECKLIST_ITEM_ID = "extra_checklist_item_id";

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
                        item.setCompleted(!item.isCompleted());
                        db.checklistItemDao().update(item);
                        updateAllWidgets(context);
                    }
                });
            }
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(new android.content.ComponentName(context, WidgetProvider.class));
        for (int id : ids) {
            updateAppWidget(context, appWidgetManager, id);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

        // Determine Layout based on size
        int layoutId = getLayoutForSize(minWidth, minHeight);
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        // Fetch Data and Update
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            List<Expense> expenses = db.expenseDao().getAllExpensesSync();
            List<com.example.expenseeye.models.Category> categories = db.categoryDao().getAllCategoriesSync();
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
                views.setImageViewBitmap(R.id.iv_widget_pie, createMiniPieChart(primary, textSecondary));
            }

            // Payment method stats
            if (hasView(layoutId, R.id.tv_widget_pay_cash)) {
                views.setTextViewText(R.id.tv_widget_pay_cash, String.format(Locale.getDefault(), "Cash: ₹%.0f", data.cashTotal));
                views.setTextViewText(R.id.tv_widget_pay_upi, String.format(Locale.getDefault(), "UPI: ₹%.0f", data.upiTotal));
                views.setTextViewText(R.id.tv_widget_pay_card, String.format(Locale.getDefault(), "Card: ₹%.0f", data.cardTotal));
            }

            // Quick Category Actions
            if (hasView(layoutId, R.id.btn_widget_action_1)) {
                setupQuickActions(context, views, categories);
            }

            // Checklist Setup
            if (hasView(layoutId, R.id.layout_widget_checklist)) {
                setupChecklistViews(context, views, checklistItems, layoutId);
            }

            // Click Intents for standard buttons
            setupClickIntents(context, views);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }).start();
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

    private static void setupQuickActions(Context context, RemoteViews views, List<com.example.expenseeye.models.Category> categories) {
        List<String> actionCategories = WidgetPreferenceManager.getQuickActions(context);
        int[] actionBtnIds = {R.id.btn_widget_action_1, R.id.btn_widget_action_2, R.id.btn_widget_action_3};
        int[] actionIvIds = {R.id.iv_widget_action_1, R.id.iv_widget_action_2, R.id.iv_widget_action_3};
        int[] actionTvIds = {R.id.tv_widget_action_1, R.id.tv_widget_action_2, R.id.tv_widget_action_3};

        for (int i = 0; i < 3; i++) {
            if (i < actionCategories.size()) {
                String catName = actionCategories.get(i);
                views.setViewVisibility(actionBtnIds[i], android.view.View.VISIBLE);
                views.setTextViewText(actionTvIds[i], catName);

                // Find icon
                int iconResId = R.drawable.ic_other;
                for (com.example.expenseeye.models.Category c : categories) {
                    if (c.getName().equalsIgnoreCase(catName)) {
                        int resId = context.getResources().getIdentifier(c.getIconName(), "drawable", context.getPackageName());
                        if (resId != 0) {
                            iconResId = resId;
                        }
                        break;
                    }
                }
                views.setImageViewResource(actionIvIds[i], iconResId);

                // Set click intent
                Intent addIntent = new Intent(context, QuickAddExpenseActivity.class);
                addIntent.putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, catName);
                PendingIntent addPI = PendingIntent.getActivity(context, 10 + i, addIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(actionBtnIds[i], addPI);
            } else {
                views.setViewVisibility(actionBtnIds[i], android.view.View.GONE);
            }
        }
    }

    private static void setupChecklistViews(Context context, RemoteViews views, List<com.example.expenseeye.models.ChecklistItem> items, int layoutId) {
        List<com.example.expenseeye.models.ChecklistItem> pendingItems = new java.util.ArrayList<>();
        for (com.example.expenseeye.models.ChecklistItem item : items) {
            if (!item.isCompleted()) {
                pendingItems.add(item);
            }
        }

        int maxItemsToShow = 2;
        if (layoutId == R.layout.widget_2x4 || layoutId == R.layout.widget_4x4) {
            maxItemsToShow = 3;
        }

        int[] itemLayoutIds = {
                R.id.layout_widget_checklist_item_1,
                R.id.layout_widget_checklist_item_2,
                R.id.layout_widget_checklist_item_3
        };
        int[] cbIds = {
                R.id.cb_widget_checklist_1,
                R.id.cb_widget_checklist_2,
                R.id.cb_widget_checklist_3
        };
        int[] titleIds = {
                R.id.tv_widget_checklist_title_1,
                R.id.tv_widget_checklist_title_2,
                R.id.tv_widget_checklist_title_3
        };
        int[] descIds = {
                R.id.tv_widget_checklist_desc_1,
                R.id.tv_widget_checklist_desc_2,
                R.id.tv_widget_checklist_desc_3
        };

        if (pendingItems.isEmpty()) {
            views.setViewVisibility(R.id.tv_widget_checklist_empty, android.view.View.VISIBLE);
            for (int i = 0; i < 3; i++) {
                if (hasView(layoutId, itemLayoutIds[i])) {
                    views.setViewVisibility(itemLayoutIds[i], android.view.View.GONE);
                }
            }
        } else {
            views.setViewVisibility(R.id.tv_widget_checklist_empty, android.view.View.GONE);
            for (int i = 0; i < 3; i++) {
                if (hasView(layoutId, itemLayoutIds[i])) {
                    if (i < pendingItems.size() && i < maxItemsToShow) {
                        com.example.expenseeye.models.ChecklistItem item = pendingItems.get(i);
                        views.setViewVisibility(itemLayoutIds[i], android.view.View.VISIBLE);
                        views.setTextViewText(titleIds[i], item.getTitle());

                        String desc = item.getPriority();
                        if (item.getQuantity() != null && !item.getQuantity().trim().isEmpty()) {
                            desc += " (" + item.getQuantity() + ")";
                        }
                        views.setTextViewText(descIds[i], desc);

                        int color = android.graphics.Color.parseColor("#60A5FA");
                        if ("HIGH".equalsIgnoreCase(item.getPriority())) {
                            color = android.graphics.Color.parseColor("#F87171");
                        } else if ("MEDIUM".equalsIgnoreCase(item.getPriority())) {
                            color = android.graphics.Color.parseColor("#FB923C");
                        }
                        views.setTextColor(descIds[i], color);

                        views.setImageViewResource(cbIds[i], item.isCompleted() ? R.drawable.ic_widget_checkbox_checked : R.drawable.ic_widget_checkbox_blank);

                        Intent toggleIntent = new Intent(context, WidgetProvider.class);
                        toggleIntent.setAction(ACTION_TOGGLE_CHECKLIST_ITEM);
                        toggleIntent.putExtra(EXTRA_CHECKLIST_ITEM_ID, item.getId());
                        PendingIntent togglePI = PendingIntent.getBroadcast(context, (int) item.getId() + 100, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        views.setOnClickPendingIntent(cbIds[i], togglePI);
                    } else {
                        views.setViewVisibility(itemLayoutIds[i], android.view.View.GONE);
                    }
                }
            }
        }
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
        if (viewId == R.id.btn_widget_action_1 || viewId == R.id.btn_widget_action_2 || viewId == R.id.btn_widget_action_3) {
            return layoutId == R.layout.widget_3x2 || layoutId == R.layout.widget_3x3 ||
                    layoutId == R.layout.widget_4x2 || layoutId == R.layout.widget_4x3 ||
                    layoutId == R.layout.widget_4x4;
        }
        if (viewId == R.id.layout_widget_checklist) {
            return layoutId == R.layout.widget_2x3 || layoutId == R.layout.widget_2x4 ||
                    layoutId == R.layout.widget_3x3 || layoutId == R.layout.widget_4x3 ||
                    layoutId == R.layout.widget_4x4;
        }
        if (viewId == R.id.layout_widget_checklist_item_1) {
            return layoutId == R.layout.widget_2x3 || layoutId == R.layout.widget_2x4 ||
                    layoutId == R.layout.widget_3x3 || layoutId == R.layout.widget_4x3 ||
                    layoutId == R.layout.widget_4x4;
        }
        if (viewId == R.id.layout_widget_checklist_item_2) {
            return layoutId == R.layout.widget_2x3 || layoutId == R.layout.widget_2x4 ||
                    layoutId == R.layout.widget_3x3 || layoutId == R.layout.widget_4x3 ||
                    layoutId == R.layout.widget_4x4;
        }
        if (viewId == R.id.layout_widget_checklist_item_3) {
            return layoutId == R.layout.widget_2x4 || layoutId == R.layout.widget_4x4;
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

    private static Bitmap createMiniPieChart(int color, int bgColor) {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(bgColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(15);
        canvas.drawCircle(50, 50, 40, paint);

        paint.setColor(color);
        canvas.drawArc(new RectF(10, 10, 90, 90), -90, 240, false, paint);
        return bitmap;
    }

    private static class WidgetData {
        double todayTotal = 0, weekTotal = 0, monthTotal = 0;
        double cashTotal = 0, upiTotal = 0, cardTotal = 0;
    }
}
