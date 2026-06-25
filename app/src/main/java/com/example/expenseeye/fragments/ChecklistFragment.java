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

public class ChecklistFragment extends Fragment {

    private AppViewModel viewModel;
    private ChecklistAdapter adapter;
    private EditText etTitle, etQty;
    private androidx.recyclerview.widget.RecyclerView rvPriority;
    private com.example.expenseeye.adapters.PrioritySelectionAdapter priorityAdapter;
    private ChipGroup cgFilters;
    private LinearLayout layoutEmpty;
    private RecyclerView rvChecklist;

    private List<ChecklistItem> rawItemsList = new ArrayList<>();
    private final String[] priorities = {"LOW", "MEDIUM", "HIGH"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_checklist, container, false);

        etTitle = view.findViewById(R.id.et_item_title);
        etQty = view.findViewById(R.id.et_item_qty);
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

                    if (isChecked) {
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
        rvChecklist.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

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
            adapter.setShoppingMode(isChecked);
            if (isChecked) {
                Toast.makeText(getContext(), "Shopping Mode Active!", Toast.LENGTH_SHORT).show();
            }
        });

        // Add Item Action
        btnAdd.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String qty = etQty.getText().toString().trim();
            String priority = priorityAdapter.getSelectedPriority();

            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Please enter an item name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Auto-categorize based on title
            com.example.expenseeye.theme.ThemePreferenceHelper prefH = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
            String category = "Other";
            if (prefH.isSmartClassifierEnabled()) {
                category = ExpenseClassifier.classifyChecklistItem(title);
            }

            ChecklistItem item = new ChecklistItem(title, category, qty, priority, false);
            viewModel.insertChecklistItem(item);

            // Reset inputs
            etTitle.setText("");
            etQty.setText("");
            priorityAdapter.setSelectedPriority(priorities[0]);
            Toast.makeText(getContext(), "Item added" + (prefH.isSmartClassifierEnabled() ? ": Classified as " + category : ""), Toast.LENGTH_SHORT).show();
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
        // Find existing categories to match
        viewModel.getEnabledCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories == null || categories.isEmpty()) return;

            // Simple classification: if item category name matches a system category
            String catName = item.getCategory();
            int catId = categories.get(0).getId();
            for (com.example.expenseeye.models.Category c : categories) {
                if (c.getName().equalsIgnoreCase(catName)) {
                    catId = c.getId();
                    catName = c.getName();
                    break;
                }
            }

            final int finalCatId = catId;
            final String finalCatName = catName;

            // For simplicity, launch Dashboard to show add dialog or show a simplified one here.
            // Let's implement a quick log with default values.
            com.example.expenseeye.theme.ThemePreferenceHelper prefHelper = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
            int defaultPmId = prefHelper.getDefaultPaymentMethodId();

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Enter Amount for " + item.getTitle())
                    .setView(R.layout.dialog_quick_amount)
                    .setPositiveButton("Log", (dialog, which) -> {
                        androidx.appcompat.app.AlertDialog d = (androidx.appcompat.app.AlertDialog) dialog;
                        EditText etAmount = d.findViewById(R.id.et_quick_amount);
                        if (etAmount != null && !etAmount.getText().toString().isEmpty()) {
                            double amount = Double.parseDouble(etAmount.getText().toString());
                            
                            com.example.expenseeye.models.Expense expense = new com.example.expenseeye.models.Expense(
                                    item.getTitle(),
                                    item.getQuantity() != null && !item.getQuantity().isEmpty() ? "Qty: " + item.getQuantity() : "",
                                    amount,
                                    System.currentTimeMillis(),
                                    finalCatId,
                                    finalCatName,
                                    defaultPmId,
                                    "Cash" // Will be updated by Repository/Classifier if possible, or leave as default
                            );
                            viewModel.insertExpense(expense);
                            Toast.makeText(getContext(), "Expense logged!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
