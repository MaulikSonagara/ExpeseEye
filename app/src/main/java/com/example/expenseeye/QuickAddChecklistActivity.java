package com.example.expenseeye;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.ChecklistItem;
import com.example.expenseeye.repository.AppRepository;
import com.example.expenseeye.utils.ExpenseClassifier;
import com.example.expenseeye.utils.KeyboardFollow;
import com.example.expenseeye.widget.WidgetProvider;
import com.google.android.material.button.MaterialButton;

public class QuickAddChecklistActivity extends AppCompatActivity {

    private AppRepository repository;
    private RelativeLayout rootLayout;
    private CardView cardQuickAdd;
    private AutoCompleteTextView etTitle;
    private EditText etQty;
    private AutoCompleteTextView spinnerCategory;
    private androidx.recyclerview.widget.RecyclerView rvPriority;
    private com.example.expenseeye.adapters.PrioritySelectionAdapter priorityAdapter;
    private MaterialButton btnCancel, btnSave;

    private final String[] priorities = {"LOW", "MEDIUM", "HIGH"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load and apply theme preference
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_add_checklist);

        // Bind views
        rootLayout = findViewById(R.id.rl_quick_add_root);
        cardQuickAdd = findViewById(R.id.card_quick_add);
        KeyboardFollow.attach(rootLayout, cardQuickAdd);
        etTitle = findViewById(R.id.et_item_title);
        etQty = findViewById(R.id.et_item_qty);
        spinnerCategory = findViewById(R.id.spinner_item_category);
        rvPriority = findViewById(R.id.rv_item_priority);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);

        // Initialize Repository
        repository = new AppRepository(getApplication());

        // Populate Priority RecyclerView selector
        rvPriority.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        priorityAdapter = new com.example.expenseeye.adapters.PrioritySelectionAdapter(java.util.Arrays.asList(priorities), priorities[0], null);
        rvPriority.setAdapter(priorityAdapter);

        // Setup Category Spinner and Title Suggestions
        AppDatabase.databaseWriteExecutor.execute(() -> {
            java.util.List<com.example.expenseeye.models.Category> cats = repository.getEnabledCategoriesSync();
            java.util.List<com.example.expenseeye.models.CategoryKeyword> kws = repository.getAllKeywordsSync();
            java.util.List<String> names = new java.util.ArrayList<>();
            for (com.example.expenseeye.models.Category c : cats) names.add(c.getName());
            
            java.util.List<String> suggestions = new java.util.ArrayList<>();
            if (kws != null) {
                for (com.example.expenseeye.models.CategoryKeyword kw : kws) {
                    if (kw.getKeyword() != null && !kw.getKeyword().trim().isEmpty()) {
                        String cleanKw = kw.getKeyword().trim();
                        if (!cleanKw.isEmpty()) {
                            cleanKw = cleanKw.substring(0, 1).toUpperCase() + cleanKw.substring(1);
                        }
                        if (!suggestions.contains(cleanKw)) {
                            suggestions.add(cleanKw);
                        }
                    }
                }
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
                spinnerCategory.setAdapter(catAdapter);
                spinnerCategory.setText("Groceries", false);

                // Setup suggestions for title input
                com.example.expenseeye.theme.ThemePreferenceHelper prefHelper = new com.example.expenseeye.theme.ThemePreferenceHelper(this);
                if (prefHelper.isChecklistTitleSuggestionsEnabled()) {
                    ArrayAdapter<String> titleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions);
                    etTitle.setAdapter(titleAdapter);
                } else {
                    etTitle.setAdapter(null);
                }

                // Smart Classifier
                etTitle.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(android.text.Editable s) {
                        com.example.expenseeye.theme.ThemePreferenceHelper ph = new com.example.expenseeye.theme.ThemePreferenceHelper(QuickAddChecklistActivity.this);
                        if (!ph.isChecklistSmartClassifierEnabled()) return;
                        
                        String classified = ExpenseClassifier.classifyExpense(s.toString(), cats, kws);
                        if (names.contains(classified)) {
                            spinnerCategory.setText(classified, false);
                        }
                    }
                });
            });
        });

        // Cancel click listener
        btnCancel.setOnClickListener(v -> dismissWithAnimation());

        // Root layout tap (dismiss when click outside dialog card)
        rootLayout.setOnClickListener(v -> dismissWithAnimation());
        cardQuickAdd.setOnClickListener(v -> {
            // Prevent clicks inside card from closing activity
        });

        // Save click listener
        btnSave.setOnClickListener(v -> saveChecklistItem());

        // Animate views entrance
        animateEntrance();
    }

    private void animateEntrance() {
        rootLayout.setAlpha(0f);
        rootLayout.animate().alpha(1f).setDuration(250).start();

        cardQuickAdd.post(() -> {
            cardQuickAdd.setScaleX(0.8f);
            cardQuickAdd.setScaleY(0.8f);
            cardQuickAdd.setAlpha(0f);
            cardQuickAdd.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .withEndAction(() -> {
                        etTitle.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(etTitle, InputMethodManager.SHOW_IMPLICIT);
                        }
                    })
                    .start();
        });
    }

    private void dismissWithAnimation() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

        rootLayout.animate().alpha(0f).setDuration(200).start();
        cardQuickAdd.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(this::finish)
                .start();
    }

    private void saveChecklistItem() {
        String titleStr = etTitle.getText().toString().trim();
        String qtyStr = etQty.getText().toString().trim();
        String categoryStr = spinnerCategory.getText().toString().trim();
        String priorityStr = priorityAdapter.getSelectedPriority();

        if (titleStr.isEmpty()) {
            Toast.makeText(this, "Please enter an item name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (categoryStr.isEmpty()) {
            categoryStr = "Other";
        }

        ChecklistItem newItem = new ChecklistItem(titleStr, categoryStr, qtyStr, priorityStr, false);

        // Save to DB in background
        String finalCategoryStr = categoryStr;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            repository.insertChecklistItem(newItem);
            runOnUiThread(() -> {
                Toast.makeText(this, "Task added: " + titleStr, Toast.LENGTH_SHORT).show();
                // Update all widgets
                WidgetProvider.updateAllWidgets(this);
                dismissWithAnimation();
            });
        });
    }

    @Override
    public void onBackPressed() {
        dismissWithAnimation();
    }
}
