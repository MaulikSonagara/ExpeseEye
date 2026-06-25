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

        btnFilter.setOnClickListener(v -> showFilterBottomSheet());
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

    private void showFilterBottomSheet() {
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
        for (Category category : availableCategories) {
            Chip chip = new Chip(getContext());
            chip.setText(category.getName());
            chip.setCheckable(true);
            chip.setChecked(currentParams.categories.contains(category.getName()));
            cgCategories.addView(chip);
        }

        // Add Payment Method Chips dynamically
        for (PaymentMethod pm : availablePaymentMethods) {
            Chip chip = new Chip(getContext());
            chip.setText(pm.getName());
            chip.setCheckable(true);
            chip.setChecked(currentParams.paymentMethods.contains(pm.getName()));
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

    private void showEditExpenseBottomSheet(Expense expense) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext(), R.style.Theme_ExpenseEye_Dialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        KeyboardFollow.attach(
                dialogView.findViewById(R.id.rl_quick_add_root),
                dialogView.findViewById(R.id.card_quick_add)
        );

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        AutoCompleteTextView etTitle = dialogView.findViewById(R.id.et_title);
        AutoCompleteTextView spinnerCategory = dialogView.findViewById(R.id.spinner_category);

        // Setup suggestions for title
        com.example.expenseeye.theme.ThemePreferenceHelper prefH = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
        if (prefH.isTitleSuggestionsEnabled()) {
            List<String> suggestions = new ArrayList<>();
            List<com.example.expenseeye.models.CategoryKeyword> kws = viewModel.getAllKeywords().getValue();
            if (kws == null) {
                kws = allKeywords;
            }
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
            ArrayAdapter<String> titleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions);
            etTitle.setAdapter(titleAdapter);
        }
        Button btnDate = dialogView.findViewById(R.id.btn_date);
        Button btnTime = dialogView.findViewById(R.id.btn_time);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete);

        if (tvTitle != null) {
            tvTitle.setVisibility(View.GONE);
        }
        etAmount.setText(String.valueOf(expense.getAmount()));
        etTitle.setText(expense.getTitle());
        etDescription.setText(expense.getDescription());

        final Calendar selectedDateTime = Calendar.getInstance();
        selectedDateTime.setTimeInMillis(expense.getTimestamp());

        // Populate Category Spinner
        List<String> catNames = new ArrayList<>();
        for (Category c : availableCategories) {
            catNames.add(c.getName());
        }
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, catNames);
        spinnerCategory.setAdapter(catAdapter);
        spinnerCategory.setText(expense.getCategoryName(), false);

        // Populate Payment Method Chips and select current
        setupPaymentMethodChipsForDialog(dialogView, expense.getPaymentMethodName());

        // Show Delete button and wire click listener
        btnDelete.setVisibility(View.VISIBLE);
        btnDelete.setOnClickListener(v -> {
            viewModel.deleteExpense(expense);
            Toast.makeText(getContext(), "Expense deleted", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Smart Classifier Text Watcher
        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                com.example.expenseeye.theme.ThemePreferenceHelper ph = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
                if (!ph.isSmartClassifierEnabled()) return;

                String titleText = s.toString();
                String descText = etDescription != null ? etDescription.getText().toString() : "";
                String classified = com.example.expenseeye.utils.ExpenseClassifier.classifyExpense(titleText + " " + descText, availableCategories, allKeywords);
                int index = catNames.indexOf(classified);
                if (index >= 0) {
                    spinnerCategory.setText(classified, false);
                }
            }
        });

        SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        btnDate.setText(sdfDate.format(selectedDateTime.getTime()));
        btnTime.setText(sdfTime.format(selectedDateTime.getTime()));

        btnDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                btnDate.setText(sdfDate.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        btnTime.setOnClickListener(v -> {
            android.app.TimePickerDialog timePicker = new android.app.TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                btnTime.setText(sdfTime.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), false);
            timePicker.show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            String titleStr = etTitle.getText().toString();
            if (amountStr.isEmpty() || titleStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in title and amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String category = spinnerCategory.getText().toString();
            
            // Get selected payment method
            String payment = getSelectedPaymentMethodFromDialog(dialogView);
            String desc = etDescription.getText().toString();

            int catId = expense.getCategoryId();
            for (Category c : availableCategories) {
                if (c.getName().equals(category)) {
                    catId = c.getId();
                    break;
                }
            }

            int pmId = expense.getPaymentMethodId();
            for (PaymentMethod pm : availablePaymentMethods) {
                if (pm.getName().equals(payment)) {
                    pmId = pm.getId();
                    break;
                }
            }

            expense.setTitle(titleStr);
            expense.setAmount(amount);
            expense.setTimestamp(selectedDateTime.getTimeInMillis());
            expense.setCategoryId(catId);
            expense.setCategoryName(category);
            expense.setPaymentMethodId(pmId);
            expense.setPaymentMethodName(payment);
            expense.setDescription(desc);

            viewModel.updateExpense(expense);
            Toast.makeText(getContext(), "Expense updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupPaymentMethodChipsForDialog(View dialogView, String selectedPaymentMethod) {
        RecyclerView rvMain = dialogView.findViewById(R.id.rv_payment_main);
        android.widget.LinearLayout layoutOther = dialogView.findViewById(R.id.layout_payment_other);
        RecyclerView rvOther = dialogView.findViewById(R.id.rv_payment_other);

        List<String> mainMethods = Arrays.asList("Cash", "UPI", "Other");
        List<String> otherMethods = Arrays.asList("Debit Card", "Credit Card", "Bank Transfer", "Wallet");

        String initialMain = "UPI";
        if (selectedPaymentMethod != null) {
            if (mainMethods.contains(selectedPaymentMethod)) initialMain = selectedPaymentMethod;
            else if (otherMethods.contains(selectedPaymentMethod)) initialMain = "Other";
        }

        final PaymentMethodAdapter[] adapterOther = new PaymentMethodAdapter[1];
        final PaymentMethodAdapter adapterMain = new PaymentMethodAdapter(mainMethods, initialMain, name -> {
            if ("Other".equals(name)) {
                layoutOther.setVisibility(View.VISIBLE);
            } else {
                layoutOther.setVisibility(View.GONE);
                if (adapterOther[0] != null) adapterOther[0].setSelectedMethod(null);
            }
        });

        rvMain.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMain.setAdapter(adapterMain);

        if ("Other".equals(initialMain)) {
            layoutOther.setVisibility(View.VISIBLE);
        }

        adapterOther[0] = new PaymentMethodAdapter(otherMethods, selectedPaymentMethod, name -> {
            // Sub-selection handled by adapter notify
        });
        rvOther.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvOther.setAdapter(adapterOther[0]);
    }

    private String getSelectedPaymentMethodFromDialog(View dialogView) {
        RecyclerView rvMain = dialogView.findViewById(R.id.rv_payment_main);
        RecyclerView rvOther = dialogView.findViewById(R.id.rv_payment_other);
        
        PaymentMethodAdapter mainAdapter = (PaymentMethodAdapter) rvMain.getAdapter();
        PaymentMethodAdapter otherAdapter = (PaymentMethodAdapter) rvOther.getAdapter();

        if (mainAdapter != null) {
            String main = mainAdapter.getSelectedMethod();
            if ("Other".equals(main) && otherAdapter != null) {
                String other = otherAdapter.getSelectedMethod();
                return (other != null) ? other : "Other";
            }
            return (main != null) ? main : "Cash";
        }
        return "Other";
    }
}
