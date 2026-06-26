package com.example.expenseeye;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.adapters.ReminderExpenseAdapter;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.models.ReminderExpense;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderExpensesActivity extends AppCompatActivity {

    private AppViewModel viewModel;
    private ReminderExpenseAdapter adapter;
    private View layoutEmpty;
    private List<Category> categories = new ArrayList<>();
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_expenses);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_reminder_expenses);
        layoutEmpty = findViewById(R.id.layout_empty_reminder);
        FloatingActionButton fab = findViewById(R.id.fab_add_reminder);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReminderExpenseAdapter(this::showAddEditDialog, this::toggleReminder);
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        viewModel.getAllReminderExpenses().observe(this, items -> {
            if (items == null || items.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
                adapter.submitList(items);
            }
        });

        viewModel.getEnabledCategories().observe(this, cats -> {
            categories = cats;
            adapter.setCategories(cats);
        });
        viewModel.getAllPaymentMethods().observe(this, pms -> paymentMethods = pms);

        fab.setOnClickListener(v -> showAddEditDialog(null));
    }

    private void toggleReminder(ReminderExpense re, boolean enabled) {
        re.setEnabled(enabled);
        viewModel.updateReminderExpense(re);
    }

    private void showAddEditDialog(ReminderExpense re) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder_expense, null);
        
        EditText etTitle = dialogView.findViewById(R.id.et_re_title);
        EditText etAmount = dialogView.findViewById(R.id.et_re_amount);
        AutoCompleteTextView spinCat = dialogView.findViewById(R.id.spinner_re_category);
        AutoCompleteTextView spinPay = dialogView.findViewById(R.id.spinner_re_payment);
        AutoCompleteTextView spinFreq = dialogView.findViewById(R.id.spinner_re_frequency);
        EditText etDueDate = dialogView.findViewById(R.id.et_re_due_date);
        EditText etDueTime = dialogView.findViewById(R.id.et_re_due_time);

        // Setup Spinners
        List<String> catNames = new ArrayList<>();
        for (Category c : categories) catNames.add(c.getName());
        spinCat.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, catNames));

        List<String> payNames = new ArrayList<>();
        for (PaymentMethod pm : paymentMethods) payNames.add(pm.getName());
        spinPay.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, payNames));

        String[] freqs = {"DAILY", "WEEKLY", "MONTHLY"};
        spinFreq.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, freqs));

        // Date and Time selection setups
        final Calendar calendar = Calendar.getInstance();
        if (re != null && re.getNextDueTimestamp() > 0) {
            calendar.setTimeInMillis(re.getNextDueTimestamp());
        } else {
            // Default to tomorrow same time
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        etDueDate.setText(dateFormat.format(calendar.getTime()));
        etDueTime.setText(timeFormat.format(calendar.getTime()));

        etDueDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                etDueDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        etDueTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                etDueTime.setText(timeFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        if (re != null) {
            etTitle.setText(re.getTitle());
            etAmount.setText(String.valueOf(re.getAmount()));
            spinCat.setText(re.getCategoryName(), false);
            spinPay.setText(re.getPaymentMethodName(), false);
            spinFreq.setText(re.getFrequency(), false);
        } else {
            spinFreq.setText(freqs[2], false); // Default Monthly
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(re == null ? "Add Reminder Expense" : "Edit Reminder Expense")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    saveReminder(re, etTitle, etAmount, spinCat, spinPay, spinFreq, calendar.getTimeInMillis());
                })
                .setNegativeButton("Cancel", null);

        if (re != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> {
                viewModel.deleteReminderExpense(re);
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
            });
        }

        builder.show();
    }

    private void saveReminder(ReminderExpense re, EditText etTitle, EditText etAmount, AutoCompleteTextView spinCat, AutoCompleteTextView spinPay, AutoCompleteTextView spinFreq, long nextDueTimestamp) {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String cat = spinCat.getText().toString();
        String pay = spinPay.getText().toString();
        String freq = spinFreq.getText().toString();

        if (title.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        int catId = -1;
        for (Category c : categories) if (c.getName().equals(cat)) catId = c.getId();
        int payId = -1;
        for (PaymentMethod pm : paymentMethods) if (pm.getName().equals(pay)) payId = pm.getId();

        if (re == null) {
            viewModel.insertReminderExpense(new ReminderExpense(title, amount, catId, cat, payId, pay, freq, nextDueTimestamp, true, 0));
        } else {
            re.setTitle(title);
            re.setAmount(amount);
            re.setCategoryId(catId);
            re.setCategoryName(cat);
            re.setPaymentMethodId(payId);
            re.setPaymentMethodName(pay);
            re.setFrequency(freq);
            re.setNextDueTimestamp(nextDueTimestamp);
            re.setType(0);
            viewModel.updateReminderExpense(re);
        }
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }
}
