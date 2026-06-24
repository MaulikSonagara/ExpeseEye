package com.example.expenseeye;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.adapters.CategoryManagementAdapter;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.CategoryKeyword;
import com.example.expenseeye.theme.ThemeManager;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CategoryManagementActivity extends AppCompatActivity {

    private AppViewModel viewModel;
    private CategoryManagementAdapter adapter;
    private List<Category> allCategories = new ArrayList<>();
    private List<CategoryKeyword> allKeywords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvCategories = findViewById(R.id.rv_categories);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add_category);

        adapter = new CategoryManagementAdapter(
                category -> {
                    // Open CategoryEditorActivity for edit
                    Intent intent = new Intent(CategoryManagementActivity.this, CategoryEditorActivity.class);
                    intent.putExtra(CategoryEditorActivity.EXTRA_CATEGORY_ID, category.getId());
                    startActivity(intent);
                },
                (category, isEnabled) -> {
                    // Prevent disabling the "Others" fallback category, as it is required for fallback grouping
                    if (!isEnabled && (category.getName().equalsIgnoreCase("Others") || category.getName().equalsIgnoreCase("Other"))) {
                        Toast.makeText(this, "The default fallback category cannot be disabled.", Toast.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                        return;
                    }
                    category.setEnabled(isEnabled);
                    viewModel.updateCategory(category);
                    Toast.makeText(this, category.getName() + (isEnabled ? " enabled" : " disabled"), Toast.LENGTH_SHORT).show();
                }
        );

        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        // Observe categories
        viewModel.getAllCategories().observe(this, categories -> {
            if (categories != null) {
                allCategories = categories;
                adapter.setData(allCategories, allKeywords);
            }
        });

        // Observe keywords
        viewModel.getAllKeywords().observe(this, keywords -> {
            if (keywords != null) {
                allKeywords = keywords;
                adapter.setData(allCategories, allKeywords);
            }
        });

        fabAdd.setOnClickListener(v -> {
            // Open CategoryEditorActivity for create
            Intent intent = new Intent(CategoryManagementActivity.this, CategoryEditorActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_category_management, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_reset) {
            showResetConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset to Default?")
                .setMessage("Are you sure you want to reset all categories to defaults? Any custom categories will be deleted, and their expenses will be migrated to the 'Others' category safely.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    viewModel.resetCategoriesToDefault();
                    Toast.makeText(this, "Resetting categories to default settings...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
