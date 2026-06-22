package com.example.expenseeye.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.adapters.ExpenseAdapter;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.utils.ExpenseClassifier;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private AppViewModel viewModel;
    private ExpenseAdapter adapter;
    private TextView tvMonthTotal, tvTodayTotal, tvWeekTotal, tvYearTotal, tvComparison, tvNoExpenses;
    private RecyclerView rvRecentExpenses;
    private List<Category> availableCategories = new ArrayList<>();
    private List<PaymentMethod> availablePaymentMethods = new ArrayList<>();

    // Variables for picker in BottomSheet dialog
    private Calendar selectedDateTime = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Bind views
        tvMonthTotal = view.findViewById(R.id.tv_month_total);
        tvTodayTotal = view.findViewById(R.id.tv_today_total);
        tvWeekTotal = view.findViewById(R.id.tv_week_total);
        tvYearTotal = view.findViewById(R.id.tv_year_total);
        tvComparison = view.findViewById(R.id.tv_comparison);
        tvNoExpenses = view.findViewById(R.id.tv_no_expenses);
        rvRecentExpenses = view.findViewById(R.id.rv_recent_expenses);
        FloatingActionButton fabAddExpense = view.findViewById(R.id.fab_add_expense);
        TextView btnSeeAll = view.findViewById(R.id.btn_see_all);

        // Set up recycler view
        rvRecentExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExpenseAdapter(this::showEditExpenseBottomSheet);
        rvRecentExpenses.setAdapter(adapter);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        // Observe Categories & PaymentMethods for spinners cache
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

        // Observe all expenses to calculate totals and list recent
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null && !expenses.isEmpty()) {
                tvNoExpenses.setVisibility(View.GONE);
                rvRecentExpenses.setVisibility(View.VISIBLE);

                // Show only up to 5 recent expenses
                List<Expense> recent = expenses.size() > 5 ? expenses.subList(0, 5) : expenses;
                adapter.submitExpenseList(recent);

                // Calculate statistics
                calculateTotals(expenses);
            } else {
                tvNoExpenses.setVisibility(View.VISIBLE);
                rvRecentExpenses.setVisibility(View.GONE);
                resetDashboardTotals();
            }
        });

        // See All action
        btnSeeAll.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.expensesFragment));

        // FAB Quick Add action
        fabAddExpense.setOnClickListener(v -> showAddExpenseBottomSheet());

        return view;
    }

    private void resetDashboardTotals() {
        tvTodayTotal.setText("₹0.00");
        tvWeekTotal.setText("₹0.00");
        tvMonthTotal.setText("₹0.00");
        tvYearTotal.setText("₹0.00");
        tvComparison.setText("Start logging expenses to see trends!");
    }

    private void calculateTotals(List<Expense> expenses) {
        double todayTotal = 0;
        double weekTotal = 0;
        double monthTotal = 0;
        double yearTotal = 0;
        double lastMonthTotal = 0;

        Calendar calToday = Calendar.getInstance();
        Calendar calWeek = Calendar.getInstance();
        calWeek.set(Calendar.DAY_OF_WEEK, calWeek.getFirstDayOfWeek());
        calWeek.set(Calendar.HOUR_OF_DAY, 0);
        calWeek.set(Calendar.MINUTE, 0);
        calWeek.set(Calendar.SECOND, 0);

        Calendar calMonth = Calendar.getInstance();
        calMonth.set(Calendar.DAY_OF_MONTH, 1);
        calMonth.set(Calendar.HOUR_OF_DAY, 0);

        Calendar calYear = Calendar.getInstance();
        calYear.set(Calendar.DAY_OF_YEAR, 1);

        // Date bounds for last month comparison
        Calendar calLastMonthStart = Calendar.getInstance();
        calLastMonthStart.add(Calendar.MONTH, -1);
        calLastMonthStart.set(Calendar.DAY_OF_MONTH, 1);
        calLastMonthStart.set(Calendar.HOUR_OF_DAY, 0);
        calLastMonthStart.set(Calendar.MINUTE, 0);
        calLastMonthStart.set(Calendar.SECOND, 0);

        Calendar calLastMonthEnd = Calendar.getInstance();
        calLastMonthEnd.set(Calendar.DAY_OF_MONTH, 1);
        calLastMonthEnd.add(Calendar.DAY_OF_MONTH, -1);
        calLastMonthEnd.set(Calendar.HOUR_OF_DAY, 23);
        calLastMonthEnd.set(Calendar.MINUTE, 59);

        for (Expense expense : expenses) {
            long ts = expense.getTimestamp();

            // Check if today
            if (isSameDay(ts, calToday.getTimeInMillis())) {
                todayTotal += expense.getAmount();
            }

            // Check if this week
            if (ts >= calWeek.getTimeInMillis()) {
                weekTotal += expense.getAmount();
            }

            // Check if this month
            if (ts >= calMonth.getTimeInMillis()) {
                monthTotal += expense.getAmount();
            }

            // Check if this year
            if (ts >= calYear.getTimeInMillis()) {
                yearTotal += expense.getAmount();
            }

            // Check if last month
            if (ts >= calLastMonthStart.getTimeInMillis() && ts <= calLastMonthEnd.getTimeInMillis()) {
                lastMonthTotal += expense.getAmount();
            }
        }

        tvTodayTotal.setText(String.format(Locale.getDefault(), "₹%.2f", todayTotal));
        tvWeekTotal.setText(String.format(Locale.getDefault(), "₹%.2f", weekTotal));
        tvMonthTotal.setText(String.format(Locale.getDefault(), "₹%.2f", monthTotal));
        tvYearTotal.setText(String.format(Locale.getDefault(), "₹%.2f", yearTotal));

        // Comparison text
        if (lastMonthTotal > 0) {
            double difference = monthTotal - lastMonthTotal;
            if (difference > 0) {
                tvComparison.setText(String.format(Locale.getDefault(), "Spent ₹%.2f more than last month (₹%.2f)", difference, lastMonthTotal));
                tvComparison.setTextColor(Color.parseColor("#FFCDD2"));
            } else {
                tvComparison.setText(String.format(Locale.getDefault(), "Saved ₹%.2f compared to last month (₹%.2f)", Math.abs(difference), lastMonthTotal));
                tvComparison.setTextColor(Color.parseColor("#C8E6C9"));
            }
        } else {
            tvComparison.setText("First month logging data. Keep it up!");
            tvComparison.setTextColor(Color.parseColor("#E8EAF6"));
        }
    }

    private boolean isSameDay(long ts1, long ts2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(ts1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(ts2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void showAddExpenseBottomSheet() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext(), R.style.Theme_ExpenseEye_Dialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        EditText etTitle = dialogView.findViewById(R.id.et_title);
        AutoCompleteTextView spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        Button btnDate = dialogView.findViewById(R.id.btn_date);
        Button btnTime = dialogView.findViewById(R.id.btn_time);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        if (tvTitle != null) {
            tvTitle.setVisibility(View.GONE);
        }
        selectedDateTime = Calendar.getInstance();

        // Populate Category Spinner
        List<String> catNames = new ArrayList<>();
        for (Category c : availableCategories) {
            catNames.add(c.getName());
        }
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, catNames);
        spinnerCategory.setAdapter(catAdapter);

        // Populate Payment Method Chips
        setupPaymentMethodChipsForDialog(dialogView, null);

        // Smart Classifier Text Watcher
        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String classified = ExpenseClassifier.classifyExpense(s.toString());
                int index = catNames.indexOf(classified);
                if (index >= 0) {
                    spinnerCategory.setText(classified, false);
                }
            }
        });

        // Date and Time button formatting
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
            TimePickerDialog timePicker = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
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
            String category = spinnerCategory.getText() != null ? spinnerCategory.getText().toString() : "Other";
            
            // Get selected payment method
            String payment = getSelectedPaymentMethodFromDialog(dialogView);
            String desc = etDescription.getText().toString();

            // Find matching category details
            int catId = 0;
            for (Category c : availableCategories) {
                if (c.getName().equals(category)) {
                    catId = c.getId();
                    break;
                }
            }

            int pmId = 0;
            for (PaymentMethod pm : availablePaymentMethods) {
                if (pm.getName().equals(payment)) {
                    pmId = pm.getId();
                    break;
                }
            }

            Expense newExpense = new Expense(
                    titleStr, desc, amount, selectedDateTime.getTimeInMillis(),
                    catId, category, pmId, payment
            );

            viewModel.insertExpense(newExpense);
            Toast.makeText(getContext(), "Expense added successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showEditExpenseBottomSheet(Expense expense) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext(), R.style.Theme_ExpenseEye_Dialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        EditText etTitle = dialogView.findViewById(R.id.et_title);
        AutoCompleteTextView spinnerCategory = dialogView.findViewById(R.id.spinner_category);
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

        selectedDateTime = Calendar.getInstance();
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

        // Date and Time button formatting
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
            TimePickerDialog timePicker = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
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

            // Find matching category details
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
        ChipGroup cgPaymentMethod = dialogView.findViewById(R.id.cg_payment_method);
        android.widget.LinearLayout layoutCardType = dialogView.findViewById(R.id.layout_card_type);
        ChipGroup cgCardType = dialogView.findViewById(R.id.cg_card_type);
        Chip chipDebitCard = dialogView.findViewById(R.id.chip_debit_card);
        Chip chipCreditCard = dialogView.findViewById(R.id.chip_credit_card);

        cgPaymentMethod.removeAllViews();
        String[] mainMethods = {"UPI", "Cash", "Debit/Credit", "Bank Transfer", "Other"};

        for (String name : mainMethods) {
            Chip chip = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
            chip.setText(name);
            chip.setCheckable(true);

            int iconResId = R.drawable.ic_other;
            if (name.equals("Cash")) {
                iconResId = R.drawable.ic_cash;
            } else if (name.equals("Debit/Credit")) {
                iconResId = R.drawable.ic_card;
            } else if (name.equals("UPI")) {
                iconResId = R.drawable.ic_upi;
            } else if (name.equals("Bank Transfer")) {
                iconResId = R.drawable.ic_bank;
            }
            chip.setChipIcon(androidx.core.content.ContextCompat.getDrawable(requireContext(), iconResId));
            chip.setChipIconVisible(true);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && name.equals("Debit/Credit")) {
                    layoutCardType.setVisibility(View.VISIBLE);
                    if (cgCardType.getCheckedChipId() == View.NO_ID) {
                        chipDebitCard.setChecked(true);
                    }
                } else if (!isChecked && name.equals("Debit/Credit")) {
                    layoutCardType.setVisibility(View.GONE);
                }
            });

            // Set checked state
            if (selectedPaymentMethod != null) {
                if (name.equals("Debit/Credit") && (selectedPaymentMethod.equalsIgnoreCase("Credit Card") || selectedPaymentMethod.equalsIgnoreCase("Debit Card"))) {
                    chip.setChecked(true);
                    layoutCardType.setVisibility(View.VISIBLE);
                    if (selectedPaymentMethod.equalsIgnoreCase("Credit Card")) {
                        chipCreditCard.setChecked(true);
                    } else {
                        chipDebitCard.setChecked(true);
                    }
                } else if (name.equals("Bank Transfer") && (selectedPaymentMethod.equalsIgnoreCase("Bank") || selectedPaymentMethod.equalsIgnoreCase("Bank Transfer"))) {
                    chip.setChecked(true);
                } else if (selectedPaymentMethod.equalsIgnoreCase(name)) {
                    chip.setChecked(true);
                }
            } else {
                // Default select UPI for new entries
                if (name.equals("UPI")) {
                    chip.setChecked(true);
                }
            }

            cgPaymentMethod.addView(chip);
        }
    }

    private String getSelectedPaymentMethodFromDialog(View dialogView) {
        ChipGroup cgPaymentMethod = dialogView.findViewById(R.id.cg_payment_method);
        ChipGroup cgCardType = dialogView.findViewById(R.id.cg_card_type);
        
        int checkedChipId = cgPaymentMethod.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip checkedChip = dialogView.findViewById(checkedChipId);
            if (checkedChip != null) {
                String checkedText = checkedChip.getText().toString();
                if (checkedText.equals("Debit/Credit")) {
                    int cardCheckedId = cgCardType.getCheckedChipId();
                    if (cardCheckedId == R.id.chip_credit_card) {
                        return "Credit Card";
                    } else {
                        return "Debit Card";
                    }
                } else {
                    return checkedText;
                }
            }
        }
        return "Other";
    }
}
