package com.example.expenseeye.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.adapters.ExpenseAdapter;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import com.example.expenseeye.adapters.PaymentMethodAdapter;
import com.example.expenseeye.utils.KeyboardFollow;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpensesFragment extends Fragment {

    private AppViewModel viewModel;
    private ExpenseAdapter adapter;
    private EditText etSearch;
    private RecyclerView rvExpenses;
    private LinearLayout layoutEmptyState;
    private ChipGroup chipGroupActiveFilters;

    private List<Category> availableCategories = new ArrayList<>();
    private List<PaymentMethod> availablePaymentMethods = new ArrayList<>();
    private List<com.example.expenseeye.models.CategoryKeyword> allKeywords = new ArrayList<>();

    // Keep track of current filter parameters
    private AppViewModel.FilterParams currentParams = new AppViewModel.FilterParams();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        etSearch = view.findViewById(R.id.et_search);
        ImageView btnFilter = view.findViewById(R.id.btn_filter);
        rvExpenses = view.findViewById(R.id.rv_expenses);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        chipGroupActiveFilters = view.findViewById(R.id.chip_group_active_filters);
        View tvClearFilters = view.findViewById(R.id.tv_clear_filters);

        rvExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExpenseAdapter(this::showEditExpenseBottomSheet);
        rvExpenses.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        // Fetch categories to populate adapter icons/colors
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                availableCategories = categories;
                adapter.setCategories(categories);
            }
        });

        viewModel.getAllPaymentMethods().observe(getViewLifecycleOwner(), pms -> {
            if (pms != null) {
                availablePaymentMethods = pms;
            }
        });

        viewModel.getAllKeywords().observe(getViewLifecycleOwner(), keywords -> {
            if (keywords != null) {
                allKeywords = keywords;
            }
        });

        // Observe filtered expenses
        viewModel.getFilteredExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null && !expenses.isEmpty()) {
                adapter.submitExpenseList(expenses);
                layoutEmptyState.setVisibility(View.GONE);
                rvExpenses.setVisibility(View.VISIBLE);
            } else {
                adapter.submitExpenseList(new ArrayList<>());
                layoutEmptyState.setVisibility(View.VISIBLE);
                rvExpenses.setVisibility(View.GONE);
            }
            updateActiveFilterChips();
        });

        // Search text watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentParams.searchQuery = s.toString();
                viewModel.updateFilters(currentParams);
            }
        });

        btnFilter.setOnClickListener(v -> {
            com.example.expenseeye.database.AppDatabase.databaseWriteExecutor.execute(() -> {
                final List<Category> allCats = viewModel.getAllCategoriesSync();
                final List<PaymentMethod> allPMs = viewModel.getAllPaymentMethodsSync();
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    showFilterBottomSheet(allCats, allPMs);
                });
            });
        });
        tvClearFilters.setOnClickListener(v -> resetFilters());



        return view;
    }

    private void resetFilters() {
        currentParams = new AppViewModel.FilterParams();
        etSearch.setText("");
        viewModel.updateFilters(currentParams);
    }

    private void updateActiveFilterChips() {
        chipGroupActiveFilters.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());
        com.example.expenseeye.theme.ThemePreferenceHelper prefHelper = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
        String currency = prefHelper.getCurrencySymbol();

        if (currentParams.startDate != null) {
            addFilterChip("After: " + sdf.format(currentParams.startDate), () -> {
                currentParams.startDate = null;
                viewModel.updateFilters(currentParams);
            });
        }

        if (currentParams.endDate != null) {
            addFilterChip("Before: " + sdf.format(currentParams.endDate), () -> {
                currentParams.endDate = null;
                viewModel.updateFilters(currentParams);
            });
        }

        if (currentParams.minAmount != null) {
            addFilterChip("Min: " + currency + currentParams.minAmount, () -> {
                currentParams.minAmount = null;
                viewModel.updateFilters(currentParams);
            });
        }

        if (currentParams.maxAmount != null) {
            addFilterChip("Max: " + currency + currentParams.maxAmount, () -> {
                currentParams.maxAmount = null;
                viewModel.updateFilters(currentParams);
            });
        }

        for (String cat : currentParams.categories) {
            addFilterChip(cat, () -> {
                currentParams.categories.remove(cat);
                viewModel.updateFilters(currentParams);
            });
        }

        for (String pm : currentParams.paymentMethods) {
            addFilterChip(pm, () -> {
                currentParams.paymentMethods.remove(pm);
                viewModel.updateFilters(currentParams);
            });
        }
    }

    private void addFilterChip(String text, Runnable onClose) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> onClose.run());
        chipGroupActiveFilters.addView(chip);
    }

    private void showFilterBottomSheet(List<Category> categoriesList, List<PaymentMethod> pmsList) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_expenses, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                        .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        Button btnStartDate = dialogView.findViewById(R.id.btn_filter_start_date);
        Button btnEndDate = dialogView.findViewById(R.id.btn_filter_end_date);
        EditText etMinAmount = dialogView.findViewById(R.id.et_filter_min_amount);
        EditText etMaxAmount = dialogView.findViewById(R.id.et_filter_max_amount);
        ChipGroup cgCategories = dialogView.findViewById(R.id.cg_filter_categories);
        ChipGroup cgPayments = dialogView.findViewById(R.id.cg_filter_payments);
        Button btnReset = dialogView.findViewById(R.id.btn_filter_reset);
        Button btnApply = dialogView.findViewById(R.id.btn_filter_apply);

        // Date selection placeholders
        final Calendar calStart = Calendar.getInstance();
        final Calendar calEnd = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        if (currentParams.startDate != null) {
            calStart.setTimeInMillis(currentParams.startDate);
            btnStartDate.setText(sdf.format(calStart.getTime()));
        }
        if (currentParams.endDate != null) {
            calEnd.setTimeInMillis(currentParams.endDate);
            btnEndDate.setText(sdf.format(calEnd.getTime()));
        }
        if (currentParams.minAmount != null) {
            etMinAmount.setText(String.valueOf(currentParams.minAmount));
        }
        if (currentParams.maxAmount != null) {
            etMaxAmount.setText(String.valueOf(currentParams.maxAmount));
        }

        btnStartDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                calStart.set(year, month, dayOfMonth, 0, 0, 0);
                btnStartDate.setText(sdf.format(calStart.getTime()));
            }, calStart.get(Calendar.YEAR), calStart.get(Calendar.MONTH), calStart.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        btnEndDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                calEnd.set(year, month, dayOfMonth, 23, 59, 59);
                btnEndDate.setText(sdf.format(calEnd.getTime()));
            }, calEnd.get(Calendar.YEAR), calEnd.get(Calendar.MONTH), calEnd.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        // Add Category Chips dynamically
        for (Category category : categoriesList) {
            Chip chip = new Chip(getContext());
            chip.setText(category.getName());
            chip.setCheckable(true);
            chip.setChecked(currentParams.categories.contains(category.getName()));
            styleFilterChip(chip);
            cgCategories.addView(chip);
        }

        // Add Payment Method Chips dynamically
        for (PaymentMethod pm : pmsList) {
            Chip chip = new Chip(getContext());
            chip.setText(pm.getName());
            chip.setCheckable(true);
            chip.setChecked(currentParams.paymentMethods.contains(pm.getName()));
            styleFilterChip(chip);
            cgPayments.addView(chip);
        }

        btnReset.setOnClickListener(v -> {
            resetFilters();
            dialog.dismiss();
        });

        btnApply.setOnClickListener(v -> {
            // Apply text and date updates
            if (btnStartDate.getText().toString().equals("Start Date")) {
                currentParams.startDate = null;
            } else {
                currentParams.startDate = calStart.getTimeInMillis();
            }

            if (btnEndDate.getText().toString().equals("End Date")) {
                currentParams.endDate = null;
            } else {
                currentParams.endDate = calEnd.getTimeInMillis();
            }

            String minStr = etMinAmount.getText().toString();
            currentParams.minAmount = minStr.isEmpty() ? null : Double.parseDouble(minStr);

            String maxStr = etMaxAmount.getText().toString();
            currentParams.maxAmount = maxStr.isEmpty() ? null : Double.parseDouble(maxStr);

            // Read Category check states
            currentParams.categories.clear();
            for (int i = 0; i < cgCategories.getChildCount(); i++) {
                Chip chip = (Chip) cgCategories.getChildAt(i);
                if (chip.isChecked()) {
                    currentParams.categories.add(chip.getText().toString());
                }
            }

            // Read Payment check states
            currentParams.paymentMethods.clear();
            for (int i = 0; i < cgPayments.getChildCount(); i++) {
                Chip chip = (Chip) cgPayments.getChildAt(i);
                if (chip.isChecked()) {
                    currentParams.paymentMethods.add(chip.getText().toString());
                }
            }

            viewModel.updateFilters(currentParams);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void styleFilterChip(Chip chip) {
        Context context = getContext();
        if (context == null) return;

        int colorPrimary = com.example.expenseeye.theme.ThemeManager.getColor(context, com.example.expenseeye.theme.ThemeManager.ThemeColor.PRIMARY);
        int colorSurface = com.example.expenseeye.theme.ThemeManager.getColor(context, com.example.expenseeye.theme.ThemeManager.ThemeColor.SURFACE);
        int colorDivider = com.example.expenseeye.theme.ThemeManager.getColor(context, com.example.expenseeye.theme.ThemeManager.ThemeColor.DIVIDER);
        int colorTextSecondary = com.example.expenseeye.theme.ThemeManager.getColor(context, com.example.expenseeye.theme.ThemeManager.ThemeColor.TEXT_SECONDARY);

        int[][] states = new int[][] {
            new int[] { android.R.attr.state_checked }, // checked
            new int[] { -android.R.attr.state_checked } // unchecked
        };

        // Background Color State List
        int[] colorsBg = new int[] {
            colorPrimary,
            Color.TRANSPARENT
        };
        ColorStateList bgStateList = new ColorStateList(states, colorsBg);

        // Text Color State List
        int[] colorsText = new int[] {
            colorSurface,
            colorTextSecondary
        };
        ColorStateList textStateList = new ColorStateList(states, colorsText);

        // Stroke Color State List
        int[] colorsStroke = new int[] {
            Color.TRANSPARENT,
            colorDivider
        };
        ColorStateList strokeStateList = new ColorStateList(states, colorsStroke);

        chip.setChipBackgroundColor(bgStateList);
        chip.setTextColor(textStateList);
        chip.setChipStrokeColor(strokeStateList);
        chip.setChipStrokeWidth(1.0f * getResources().getDisplayMetrics().density);
    }

    private void showEditExpenseBottomSheet(Expense expense) {
        com.example.expenseeye.utils.ExpenseDialogHelper.showExpenseDialog(requireContext(), getLayoutInflater(), viewModel, expense, null, null);
    }
}
