package com.example.expenseeye;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.adapters.RecurringExpenseAdapter;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.models.RecurringExpense;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class RecurringExpensesActivity extends AppCompatActivity {

    private AppViewModel viewModel;
    private RecurringExpenseAdapter adapter;
    private View layoutEmpty;
    private List<Category> categories = new ArrayList<>();
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_expenses);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_recurring_expenses);
        layoutEmpty = findViewById(R.id.layout_empty_recurring);
        FloatingActionButton fab = findViewById(R.id.fab_add_recurring);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecurringExpenseAdapter(this::showAddEditDialog, this::toggleRecurring);
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        viewModel.getAllRecurringExpenses().observe(this, items -> {
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

    private void toggleRecurring(RecurringExpense re, boolean enabled) {
        re.setEnabled(enabled);
        viewModel.updateRecurringExpense(re);
    }

    private void showAddEditDialog(RecurringExpense re) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_recurring_expense, null);
        
        EditText etTitle = dialogView.findViewById(R.id.et_re_title);
        EditText etAmount = dialogView.findViewById(R.id.et_re_amount);
        AutoCompleteTextView spinCat = dialogView.findViewById(R.id.spinner_re_category);
        AutoCompleteTextView spinPay = dialogView.findViewById(R.id.spinner_re_payment);
        AutoCompleteTextView spinFreq = dialogView.findViewById(R.id.spinner_re_frequency);

        // Setup Spinners
        List<String> catNames = new ArrayList<>();
        for (Category c : categories) catNames.add(c.getName());
        spinCat.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, catNames));

        List<String> payNames = new ArrayList<>();
        for (PaymentMethod pm : paymentMethods) payNames.add(pm.getName());
        spinPay.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, payNames));

        String[] freqs = {"DAILY", "WEEKLY", "MONTHLY"};
        spinFreq.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, freqs));

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
                .setTitle(re == null ? "Add Recurring Expense" : "Edit Recurring Expense")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    saveRecurring(re, etTitle, etAmount, spinCat, spinPay, spinFreq);
                })
                .setNegativeButton("Cancel", null);

        if (re != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> {
                viewModel.deleteRecurringExpense(re);
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
            });
        }

        builder.show();
    }

    private void saveRecurring(RecurringExpense re, EditText etTitle, EditText etAmount, AutoCompleteTextView spinCat, AutoCompleteTextView spinPay, AutoCompleteTextView spinFreq) {
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
            viewModel.insertRecurringExpense(new RecurringExpense(title, amount, catId, cat, payId, pay, freq, true));
        } else {
            re.setTitle(title);
            re.setAmount(amount);
            re.setCategoryId(catId);
            re.setCategoryName(cat);
            re.setPaymentMethodId(payId);
            re.setPaymentMethodName(pay);
            re.setFrequency(freq);
            viewModel.updateRecurringExpense(re);
        }
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }
}
