package com.example.expenseeye.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.Category;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class WidgetConfigActivity extends AppCompatActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private List<Category> allCategories = new ArrayList<>();
    private final List<String> selectedCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply dark mode preference
        android.content.SharedPreferences sharedPrefs = getSharedPreferences("ExpenseEyePrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPrefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);

        // Set default return code to CANCELLED in case user exits before saving
        setResult(RESULT_CANCELED);

        // Find widget ID from intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        recyclerView = findViewById(R.id.rv_widget_config_categories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load currently configured actions
        selectedCategories.addAll(WidgetPreferenceManager.getQuickActions(this));

        // Fetch categories from DB
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            List<Category> categories = db.categoryDao().getAllCategoriesSync();
            runOnUiThread(() -> {
                allCategories = categories;
                adapter = new CategoryAdapter();
                recyclerView.setAdapter(adapter);
            });
        });

        MaterialButton btnCancel = findViewById(R.id.btn_widget_config_cancel);
        btnCancel.setOnClickListener(v -> finish());

        MaterialButton btnSave = findViewById(R.id.btn_widget_config_save);
        btnSave.setOnClickListener(v -> {
            // Save to prefs
            WidgetPreferenceManager.saveQuickActions(this, selectedCategories);

            // Update all widgets
            WidgetProvider.updateAllWidgets(this);

            // Return OK
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        });
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_widget_config_category, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Category category = allCategories.get(position);
            holder.tvName.setText(category.getName());

            // Check if selected and find its position in selection list
            int selectIndex = selectedCategories.indexOf(category.getName());
            boolean isChecked = selectIndex != -1;

            if (isChecked) {
                holder.tvBadge.setVisibility(View.VISIBLE);
                holder.viewUnselected.setVisibility(View.GONE);
                holder.tvBadge.setText(String.valueOf(selectIndex + 1));
                
                // Highlight selected card
                holder.cardView.setStrokeColor(Color.parseColor("#6C7CFF"));
                holder.cardView.setStrokeWidth(4);
            } else {
                holder.tvBadge.setVisibility(View.GONE);
                holder.viewUnselected.setVisibility(View.VISIBLE);
                
                // Standard card border
                holder.cardView.setStrokeColor(Color.parseColor("#1F000000"));
                holder.cardView.setStrokeWidth(2);
            }

            // Set icon and dynamic background color matching the category
            int resId = getResources().getIdentifier(category.getIconName(), "drawable", getPackageName());
            if (resId != 0) {
                holder.ivIcon.setImageResource(resId);
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_other);
            }

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(category.getColor());
            holder.layoutIconBg.setBackground(shape);

            holder.itemView.setOnClickListener(v -> {
                int index = selectedCategories.indexOf(category.getName());
                if (index != -1) {
                    // Remove selection
                    selectedCategories.remove(index);
                } else {
                    // Add selection
                    if (selectedCategories.size() >= 3) {
                        Toast.makeText(WidgetConfigActivity.this, "You can select up to 3 shortcuts.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    selectedCategories.add(category.getName());
                }
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return allCategories.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            FrameLayout layoutIconBg;
            ImageView ivIcon;
            TextView tvName;
            TextView tvBadge;
            View viewUnselected;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.card_widget_config_category);
                layoutIconBg = itemView.findViewById(R.id.layout_cat_icon_bg);
                ivIcon = itemView.findViewById(R.id.iv_cat_icon);
                tvName = itemView.findViewById(R.id.tv_cat_name);
                tvBadge = itemView.findViewById(R.id.tv_cat_badge);
                viewUnselected = itemView.findViewById(R.id.view_cat_unselected);
            }
        }
    }
}
