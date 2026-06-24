package com.example.expenseeye.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.expenseeye.R;
import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.ChecklistItem;

import java.util.ArrayList;
import java.util.List;

public class WidgetChecklistService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ChecklistRemoteViewsFactory(this.getApplicationContext());
    }

    private static class ChecklistRemoteViewsFactory implements RemoteViewsFactory {
        private final Context context;
        private final List<ChecklistItem> items = new ArrayList<>();

        public ChecklistRemoteViewsFactory(Context context) {
            this.context = context;
        }

        @Override
        public void onCreate() {
            // No-op. loadData() will be called by the system on a background thread
            // via onDataSetChanged() or when the first item is requested.
        }

        @Override
        public void onDataSetChanged() {
            loadData();
        }

        private void loadData() {
            AppDatabase db = AppDatabase.getDatabase(context);
            List<ChecklistItem> allItems = db.checklistItemDao().getAllChecklistItemsSync();
            items.clear();
            for (ChecklistItem item : allItems) {
                if (!item.isCompleted()) {
                    items.add(item);
                }
            }
        }

        @Override
        public void onDestroy() {
            items.clear();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= items.size()) {
                return null;
            }

            ChecklistItem item = items.get(position);
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_checklist_item);
            row.setTextViewText(R.id.tv_widget_checklist_title, item.getTitle());

            String desc = item.getPriority();
            if (item.getQuantity() != null && !item.getQuantity().trim().isEmpty()) {
                desc += " (" + item.getQuantity() + ")";
            }
            row.setTextViewText(R.id.tv_widget_checklist_desc, desc);

            int color = Color.parseColor("#60A5FA");
            if ("HIGH".equalsIgnoreCase(item.getPriority())) {
                color = Color.parseColor("#F87171");
            } else if ("MEDIUM".equalsIgnoreCase(item.getPriority())) {
                color = Color.parseColor("#FB923C");
            }
            row.setTextColor(R.id.tv_widget_checklist_desc, color);

            row.setImageViewResource(R.id.cb_widget_checklist, item.isCompleted() ? R.drawable.ic_widget_checkbox_checked : R.drawable.ic_widget_checkbox_blank);

            // Set fill-in intent to toggle status
            Intent fillInIntent = new Intent();
            fillInIntent.putExtra(WidgetProvider.EXTRA_CHECKLIST_ITEM_ID, item.getId());
            row.setOnClickFillInIntent(R.id.cb_widget_checklist, fillInIntent);

            return row;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            if (position >= 0 && position < items.size()) {
                return items.get(position).getId();
            }
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
