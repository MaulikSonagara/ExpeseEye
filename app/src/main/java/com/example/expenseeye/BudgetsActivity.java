package com.example.expenseeye;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.adapters.BudgetAdapter;
import com.example.expenseeye.models.Budget;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BudgetsActivity extends AppCompatActivity {

    private AppViewModel viewModel;
    private BudgetAdapter adapter;
    private View layoutEmpty;
    private List<Category> categories = new ArrayList<>();
    private String currentMonthStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budgets);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_budgets);
        layoutEmpty = findViewById(R.id.layout_empty_budgets);
        FloatingActionButton fab = findViewById(R.id.fab_add_budget);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BudgetAdapter(this::showAddEditBudgetDialog);
        rv.setAdapter(adapter);

        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
        currentMonthStr = sdf.format(new Date());

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        viewModel.getBudgetsForMonth(currentMonthStr).observe(this, budgets -> {
            if (budgets == null || budgets.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
                adapter.submitList(budgets);
            }
        });

        viewModel.getEnabledCategories().observe(this, cats -> categories = cats);

        fab.setOnClickListener(v -> showAddEditBudgetDialog(null));
    }

    private void showAddEditBudgetDialog(Budget budget) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_budgets, null);
        
        EditText etAmount = dialogView.findViewById(R.id.et_budget_amount);
        AutoCompleteTextView spinCat = dialogView.findViewById(R.id.spinner_budget_category);
        View btnDelete = dialogView.findViewById(R.id.btn_delete_budget);
        View btnSave = dialogView.findViewById(R.id.btn_save_budget);

        // Setup Spinner
        List<String> names = new ArrayList<>();
        names.add("Overall");
        for (Category c : categories) names.add(c.getName());
        spinCat.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));

        if (budget != null) {
            etAmount.setText(String.valueOf(budget.getAmount()));
            spinCat.setText(budget.getCategoryName(), false);
            spinCat.setEnabled(false); // Can't change category of existing budget
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            spinCat.setText("Overall", false);
            btnDelete.setVisibility(View.GONE);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(budget == null ? "Add Budget" : "Edit Budget")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .show();

        btnSave.setOnClickListener(v -> {
            String amtStr = etAmount.getText().toString().trim();
            if (amtStr.isEmpty()) return;
            
            double amount = Double.parseDouble(amtStr);
            String cat = spinCat.getText().toString();

            if (budget == null) {
                // Check if budget for this category already exists
                new Thread(() -> {
                    Budget existing = viewModel.getBudgetSync(currentMonthStr, cat);
                    runOnUiThread(() -> {
                        if (existing != null) {
                            Toast.makeText(this, "Budget for " + cat + " already exists!", Toast.LENGTH_SHORT).show();
                        } else {
                            viewModel.insertBudget(new Budget(amount, cat, currentMonthStr));
                            dialog.dismiss();
                        }
                    });
                }).start();
            } else {
                budget.setAmount(amount);
                viewModel.updateBudget(budget);
                dialog.dismiss();
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (budget != null) {
                viewModel.deleteBudget(budget);
                dialog.dismiss();
            }
        });
    }
}
