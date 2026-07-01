package com.example.expenseeye.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.adapters.ChecklistAdapter;
import com.example.expenseeye.models.ChecklistItem;
import com.example.expenseeye.utils.ExpenseClassifier;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Arrays;
import com.example.expenseeye.adapters.PaymentMethodAdapter;
import com.example.expenseeye.utils.KeyboardFollow;

public class ChecklistFragment extends Fragment {

    private AppViewModel viewModel;
    private ChecklistAdapter adapter;
    private AutoCompleteTextView etTitle;
    private EditText etQty;
    private AutoCompleteTextView spinnerCategory;
    private androidx.recyclerview.widget.RecyclerView rvPriority;
    private com.example.expenseeye.adapters.PrioritySelectionAdapter priorityAdapter;
    private ChipGroup cgFilters;
    private LinearLayout layoutEmpty;
    private RecyclerView rvChecklist;

    private List<ChecklistItem> rawItemsList = new ArrayList<>();
    private final String[] priorities = {"LOW", "MEDIUM", "HIGH"};

    private List<com.example.expenseeye.models.Category> availableCategories = new ArrayList<>();
    private List<com.example.expenseeye.models.PaymentMethod> availablePaymentMethods = new ArrayList<>();
    private List<com.example.expenseeye.models.CategoryKeyword> allKeywords = new ArrayList<>();
    private List<com.example.expenseeye.models.Category> enabledCategoriesList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_checklist, container, false);

        etTitle = view.findViewById(R.id.et_item_title);
        etQty = view.findViewById(R.id.et_item_qty);
        spinnerCategory = view.findViewById(R.id.spinner_item_category);
        rvPriority = view.findViewById(R.id.rv_item_priority);
        Button btnAdd = view.findViewById(R.id.btn_add_item);
        cgFilters = view.findViewById(R.id.cg_checklist_filters);
        MaterialSwitch switchShopping = view.findViewById(R.id.switch_shopping_mode);
        layoutEmpty = view.findViewById(R.id.layout_checklist_empty);
        rvChecklist = view.findViewById(R.id.rv_checklist);

        // Populate Priority RecyclerView selector
        rvPriority.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        priorityAdapter = new com.example.expenseeye.adapters.PrioritySelectionAdapter(java.util.Arrays.asList(priorities), priorities[0], null);
        rvPriority.setAdapter(priorityAdapter);

        com.example.expenseeye.theme.ThemePreferenceHelper ph = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
        boolean initialShopping = ph.isShoppingModeEnabled();
        switchShopping.setChecked(initialShopping);

        // Setup Recycler
        rvChecklist.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChecklistAdapter(new ChecklistAdapter.OnChecklistItemClickListener() {
            @Override
            public void onCheckChanged(ChecklistItem item, boolean isChecked) {
                if (item.isCompleted() != isChecked) {
                    ChecklistItem updated = new ChecklistItem(
                            item.getTitle(),
                            item.getCategory(),
                            item.getQuantity(),
                            item.getPriority(),
                            isChecked
                    );
                    updated.setId(item.getId());
                    viewModel.updateChecklistItem(updated);

                    if (isChecked && !switchShopping.isChecked()) {
                        showLogAsExpenseDialog(item);
                    }
                }
            }

            @Override
            public void onDeleteClick(ChecklistItem item) {
                viewModel.deleteChecklistItem(item);
                Toast.makeText(getContext(), "Item deleted", Toast.LENGTH_SHORT).show();
            }
        });
        adapter.setShoppingMode(initialShopping);
        rvChecklist.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        // Fetch categories, payment methods, and keywords to cache/populate
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                availableCategories = categories;
            }
        });

        viewModel.getAllPaymentMethods().observe(getViewLifecycleOwner(), pms -> {
            if (pms != null) {
                availablePaymentMethods = pms;
            }
        });

        // Setup Category Spinner and cache enabled categories list
        final List<String> catNames = new ArrayList<>();
        viewModel.getEnabledCategories().observe(getViewLifecycleOwner(), cats -> {
            if (cats != null) {
                enabledCategoriesList = cats;
                catNames.clear();
                for (com.example.expenseeye.models.Category c : cats) catNames.add(c.getName());
                ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, catNames);
                spinnerCategory.setAdapter(catAdapter);
                if (spinnerCategory.getText().toString().isEmpty()) {
                    spinnerCategory.setText("Groceries", false);
                }
            }
        });

        // Observe keywords for Title Suggestions
        viewModel.getAllKeywords().observe(getViewLifecycleOwner(), keywords -> {
            if (keywords != null) {
                allKeywords = keywords;
                setupTitleSuggestions();
            }
        });

        // Smart Classifier for Title
        etTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                com.example.expenseeye.theme.ThemePreferenceHelper ph = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
                if (!ph.isChecklistSmartClassifierEnabled()) return;

                String title = s.toString();
                if (title.isEmpty()) return;

                String classified = ExpenseClassifier.classifyExpense(title, enabledCategoriesList, allKeywords);
                if (catNames.contains(classified)) {
                    spinnerCategory.setText(classified, false);
                }
            }
        });

        // Observe items
        viewModel.getAllChecklistItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                rawItemsList = items;
                applyCurrentFilters();
            }
        });

        // Filter chips logic
        cgFilters.setOnCheckedStateChangeListener((group, checkedIds) -> applyCurrentFilters());

        // Shopping Mode Toggle logic
        switchShopping.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ph.setShoppingModeEnabled(isChecked);
            adapter.setShoppingMode(isChecked);
            if (isChecked) {
                Toast.makeText(getContext(), "Shopping Mode Active!", Toast.LENGTH_SHORT).show();
            }
        });

        // Add Item Action
        btnAdd.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String qty = etQty.getText().toString().trim();
            String category = spinnerCategory.getText().toString().trim();
            String priority = priorityAdapter.getSelectedPriority();

            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Please enter an item name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (category.isEmpty()) {
                category = "Other";
            }

            ChecklistItem item = new ChecklistItem(title, category, qty, priority, false);
            viewModel.insertChecklistItem(item);

            // Reset inputs
            etTitle.setText("");
            etQty.setText("");
            priorityAdapter.setSelectedPriority(priorities[0]);
            Toast.makeText(getContext(), "Item added", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void applyCurrentFilters() {
        int checkedId = cgFilters.getCheckedChipId();
        List<ChecklistItem> filtered = new ArrayList<>();

        if (checkedId == R.id.chip_filter_pending) {
            for (ChecklistItem item : rawItemsList) {
                if (!item.isCompleted()) {
                    filtered.add(item);
                }
            }
        } else if (checkedId == R.id.chip_filter_completed) {
            for (ChecklistItem item : rawItemsList) {
                if (item.isCompleted()) {
                    filtered.add(item);
                }
            }
        } else {
            filtered.addAll(rawItemsList); // All
        }

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvChecklist.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvChecklist.setVisibility(View.VISIBLE);
        }

        adapter.submitList(filtered);
    }

    private void setupTitleSuggestions() {
        if (getContext() == null || etTitle == null) return;
        com.example.expenseeye.theme.ThemePreferenceHelper prefHelper = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
        if (prefHelper.isChecklistTitleSuggestionsEnabled()) {
            List<String> suggestions = new ArrayList<>();
            for (com.example.expenseeye.models.CategoryKeyword kw : allKeywords) {
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
            ArrayAdapter<String> titleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions);
            etTitle.setAdapter(titleAdapter);
        } else {
            etTitle.setAdapter(null);
        }
    }

    private void showLogAsExpenseDialog(com.example.expenseeye.models.ChecklistItem item) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log as Expense?")
                .setMessage("Do you want to log '" + item.getTitle() + "' as an expense?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    showAddExpenseDialog(item);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showAddExpenseDialog(com.example.expenseeye.models.ChecklistItem item) {
        com.example.expenseeye.utils.ExpenseDialogHelper.showExpenseDialog(requireContext(), getLayoutInflater(), viewModel, null, item, null);
    }
}
