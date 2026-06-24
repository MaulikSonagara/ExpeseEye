package com.example.expenseeye;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.adapters.ColorPickerAdapter;
import com.example.expenseeye.adapters.IconPickerAdapter;
import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.CategoryKeyword;
import com.example.expenseeye.theme.ThemeManager;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CategoryEditorActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "extra_category_id";

    private AppViewModel viewModel;
    private AppDatabase database;

    private Toolbar toolbar;
    private FrameLayout previewIconContainer;
    private ImageView ivPreviewIcon;
    private TextView tvPreviewName;
    private TextInputLayout tilCategoryName;
    private EditText etCategoryName;
    private RecyclerView rvColors;
    private RecyclerView rvIcons;
    private TextInputLayout tilKeyword;
    private EditText etKeyword;
    private MaterialButton btnAddKeyword;
    private ChipGroup cgKeywords;
    private MaterialButton btnSaveCategory;
    private MaterialButton btnDeleteCategory;

    private ColorPickerAdapter colorAdapter;
    private IconPickerAdapter iconAdapter;

    private int categoryId = -1;
    private boolean isEditMode = false;
    private Category editingCategory;

    private final List<String> activeKeywords = new ArrayList<>();
    private int selectedColor = 0xFFFF5722; // Default orange
    private String selectedIcon = "ic_groceries"; // Default food icon

    // Curated color list
    private final List<Integer> colorList = Arrays.asList(
            0xFFFF5722, // Deep Orange
            0xFF4CAF50, // Green
            0xFFE91E63, // Pink
            0xFFFF9800, // Orange
            0xFF009688, // Teal
            0xFF9C27B0, // Purple
            0xFF2196F3, // Blue
            0xFF3F51B5, // Indigo
            0xFF795548, // Brown
            0xFF9E9E9E, // Grey
            0xFFF44336, // Red
            0xFF00BCD4, // Cyan
            0xFF607D8B  // Blue Grey
    );

    // List of drawable icon resource names
    private final List<String> iconList = Arrays.asList(
            "ic_food",
            "ic_groceries",
            "ic_bills",
            "ic_travel",
            "ic_transport",
            "ic_shopping",
            "ic_medical",
            "ic_entertainment",
            "ic_bank",
            "ic_card",
            "ic_education",
            "ic_rent",
            "ic_internet",
            "ic_calendar",
            "ic_checklist",
            "ic_expenses",
            "ic_other"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_editor);

        database = AppDatabase.getDatabase(this);
        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        bindViews();
        setupToolbar();
        setupPreview();
        setupColorPicker();
        setupIconPicker();
        setupKeywordManager();

        categoryId = getIntent().getIntExtra(EXTRA_CATEGORY_ID, -1);
        if (categoryId != -1) {
            isEditMode = true;
            toolbar.setTitle("Edit Category");
            loadCategoryData();
        } else {
            isEditMode = false;
            toolbar.setTitle("Add Category");
            updatePreviewColorsAndIcon();
        }

        btnSaveCategory.setOnClickListener(v -> saveCategory());
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        previewIconContainer = findViewById(R.id.preview_icon_container);
        ivPreviewIcon = findViewById(R.id.iv_preview_icon);
        tvPreviewName = findViewById(R.id.tv_preview_name);
        tilCategoryName = findViewById(R.id.til_category_name);
        etCategoryName = findViewById(R.id.et_category_name);
        rvColors = findViewById(R.id.rv_colors);
        rvIcons = findViewById(R.id.rv_icons);
        tilKeyword = findViewById(R.id.til_keyword);
        etKeyword = findViewById(R.id.et_keyword);
        btnAddKeyword = findViewById(R.id.btn_add_keyword);
        cgKeywords = findViewById(R.id.cg_keywords);
        btnSaveCategory = findViewById(R.id.btn_save_category);
        btnDeleteCategory = findViewById(R.id.btn_delete_category);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupPreview() {
        etCategoryName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                tvPreviewName.setText(text.isEmpty() ? "Category Name" : text);
            }
        });
    }

    private void setupColorPicker() {
        colorAdapter = new ColorPickerAdapter(colorList, selectedColor, color -> {
            selectedColor = color;
            updatePreviewColorsAndIcon();
            if (iconAdapter != null) {
                iconAdapter.setCategoryColor(color);
            }
        });
        rvColors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvColors.setAdapter(colorAdapter);
    }

    private void setupIconPicker() {
        iconAdapter = new IconPickerAdapter(iconList, selectedIcon, selectedColor, iconName -> {
            selectedIcon = iconName;
            updatePreviewColorsAndIcon();
        });
        rvIcons.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvIcons.setAdapter(iconAdapter);
    }

    private void updatePreviewColorsAndIcon() {
        // Draw the preview icon container background circle
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(selectedColor);
        previewIconContainer.setBackground(shape);

        // Load Icon
        int resId = getResources().getIdentifier(selectedIcon, "drawable", getPackageName());
        if (resId != 0) {
            ivPreviewIcon.setImageResource(resId);
        } else {
            ivPreviewIcon.setImageResource(R.drawable.ic_other);
        }
    }

    private void setupKeywordManager() {
        btnAddKeyword.setOnClickListener(v -> {
            String kw = etKeyword.getText().toString().trim().toLowerCase();
            if (kw.isEmpty()) {
                tilKeyword.setError("Keyword cannot be empty");
                return;
            }
            tilKeyword.setError(null);
            if (activeKeywords.contains(kw)) {
                Toast.makeText(this, "Keyword already added", Toast.LENGTH_SHORT).show();
                return;
            }
            addKeywordChip(kw);
            etKeyword.setText("");
        });
    }

    private void addKeywordChip(String keyword) {
        Chip chip = new Chip(this);
        chip.setText(keyword);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            cgKeywords.removeView(chip);
            activeKeywords.remove(keyword);
        });
        cgKeywords.addView(chip);
        activeKeywords.add(keyword);
    }

    private void loadCategoryData() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            editingCategory = database.categoryDao().getById(categoryId);
            List<CategoryKeyword> keywords = database.categoryKeywordsDao().getKeywordsForCategorySync(categoryId);

            runOnUiThread(() -> {
                if (editingCategory != null) {
                    selectedColor = editingCategory.getColor();
                    selectedIcon = editingCategory.getIconName();

                    etCategoryName.setText(editingCategory.getName());
                    tvPreviewName.setText(editingCategory.getName());

                    colorAdapter.setSelectedColor(selectedColor);
                    iconAdapter.setSelectedIconName(selectedIcon);
                    iconAdapter.setCategoryColor(selectedColor);
                    updatePreviewColorsAndIcon();

                    // Load chips
                    cgKeywords.removeAllViews();
                    activeKeywords.clear();
                    for (CategoryKeyword kw : keywords) {
                        addKeywordChip(kw.getKeyword());
                    }

                    // Show delete button for all categories except the "Others" fallback category
                    if (!editingCategory.getName().equalsIgnoreCase("Others") && !editingCategory.getName().equalsIgnoreCase("Other")) {
                        btnDeleteCategory.setVisibility(View.VISIBLE);
                        btnDeleteCategory.setOnClickListener(v -> showDeleteConfirmationDialog());
                    } else {
                        btnDeleteCategory.setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Category?")
                .setMessage("Are you sure you want to delete this category? All expenses categorized under '" + editingCategory.getName() + "' will be migrated to the 'Others' category.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteCategoryAndMoveExpenses(editingCategory);
                    Toast.makeText(this, "Category deleted. Expenses moved to 'Others'.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveCategory() {
        String name = etCategoryName.getText().toString().trim();
        if (name.isEmpty()) {
            tilCategoryName.setError("Name cannot be empty");
            return;
        }
        tilCategoryName.setError(null);

        // System category renaming validation (ensure default fallback category name or general default categories aren't duplicated)
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Category existing = database.categoryDao().getByName(name);
            if (existing != null && (!isEditMode || existing.getId() != categoryId)) {
                runOnUiThread(() -> Toast.makeText(CategoryEditorActivity.this, "A category with this name already exists.", Toast.LENGTH_SHORT).show());
                return;
            }

            if (isEditMode && editingCategory != null) {
                editingCategory.setName(name);
                editingCategory.setColor(selectedColor);
                editingCategory.setIconName(selectedIcon);

                database.categoryDao().update(editingCategory);

                // Re-insert keywords
                database.categoryKeywordsDao().deleteKeywordsForCategory(categoryId);
                for (String kw : activeKeywords) {
                    database.categoryKeywordsDao().insert(new CategoryKeyword(categoryId, kw));
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                Category newCategory = new Category(name, selectedIcon, selectedColor, false);
                newCategory.setEnabled(true);
                long newId = database.categoryDao().insert(newCategory);

                for (String kw : activeKeywords) {
                    database.categoryKeywordsDao().insert(new CategoryKeyword((int) newId, kw));
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Category created", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
}
