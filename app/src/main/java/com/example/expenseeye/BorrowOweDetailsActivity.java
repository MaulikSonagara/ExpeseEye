package com.example.expenseeye;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.adapters.BorrowOwePaymentAdapter;
import com.example.expenseeye.models.BorrowOwe;
import com.example.expenseeye.models.BorrowOwePayment;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.theme.ThemePreferenceHelper;
import com.example.expenseeye.theme.ThemeManager;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BorrowOweDetailsActivity extends AppCompatActivity {

    private AppViewModel viewModel;
    private ThemePreferenceHelper preferenceHelper;
    private long borrowOweId = -1;
    private BorrowOwe currentItem;
    private String currencySymbol = "₹";
    
    private TextView tvName, tvDesc, tvTypeBadge, tvStatusBadge;
    private TextView tvInitialAmount, tvPaidAmount, tvRemainingAmount, tvDueDate, tvPercentage, tvNoPayments;
    private LinearProgressIndicator progressSettlement;
    private RecyclerView rvPaymentLogs;
    private MaterialButton btnRecordPayment, btnSettleFull;
    
    private BorrowOwePaymentAdapter adapter;
    private List<Category> availableCategories = new ArrayList<>();
    private List<PaymentMethod> availablePaymentMethods = new ArrayList<>();
    
    private double currentRemaining = 0.0;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_borrow_owe_details);

        borrowOweId = getIntent().getLongExtra("borrow_owe_id", -1);
        if (borrowOweId == -1) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        preferenceHelper = new ThemePreferenceHelper(this);
        currencySymbol = preferenceHelper.getCurrencySymbol();

        tvName = findViewById(R.id.tv_detail_name);
        tvDesc = findViewById(R.id.tv_detail_desc);
        tvTypeBadge = findViewById(R.id.tv_detail_type_badge);
        tvStatusBadge = findViewById(R.id.tv_detail_status_badge);
        tvInitialAmount = findViewById(R.id.tv_detail_initial_amount);
        tvPaidAmount = findViewById(R.id.tv_detail_paid_amount);
        tvRemainingAmount = findViewById(R.id.tv_detail_remaining_amount);
        tvDueDate = findViewById(R.id.tv_detail_due_date);
        tvPercentage = findViewById(R.id.tv_detail_percentage);
        tvNoPayments = findViewById(R.id.tv_no_payments);
        progressSettlement = findViewById(R.id.progress_settlement);
        rvPaymentLogs = findViewById(R.id.rv_payment_logs);
        btnRecordPayment = findViewById(R.id.btn_record_payment);
        btnSettleFull = findViewById(R.id.btn_settle_full);

        rvPaymentLogs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BorrowOwePaymentAdapter(currencySymbol, this::showDeletePaymentConfirmation);
        rvPaymentLogs.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        // Load details
        viewModel.getBorrowOweById(borrowOweId).observe(this, item -> {
            if (item == null) {
                // If deleted, close
                finish();
                return;
            }
            currentItem = item;
            updateUI();
        });

        // Load payments
        viewModel.getPaymentsForBorrowOwe(borrowOweId).observe(this, payments -> {
            if (payments != null) {
                adapter.submitList(payments);
                tvNoPayments.setVisibility(payments.isEmpty() ? View.VISIBLE : View.GONE);
                calculateSettlement(payments);
            }
        });

        // Cache spinners
        viewModel.getEnabledCategories().observe(this, cats -> {
            if (cats != null) availableCategories = cats;
        });

        viewModel.getAllPaymentMethods().observe(this, pms -> {
            if (pms != null) availablePaymentMethods = pms;
        });

        btnRecordPayment.setOnClickListener(v -> showRecordPaymentDialog(0.0));
        btnSettleFull.setOnClickListener(v -> showRecordPaymentDialog(currentRemaining));
    }

    private void updateUI() {
        tvName.setText(currentItem.getPersonName());
        
        if (currentItem.getDescription() != null && !currentItem.getDescription().trim().isEmpty()) {
            tvDesc.setVisibility(View.VISIBLE);
            tvDesc.setText(currentItem.getDescription());
        } else {
            tvDesc.setVisibility(View.GONE);
        }

        tvInitialAmount.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, currentItem.getAmount()));

        int dangerColor = ThemeManager.getColor(this, ThemeManager.ThemeColor.DANGER);
        int successColor = ThemeManager.getColor(this, ThemeManager.ThemeColor.SUCCESS);

        if (currentItem.isBorrow()) {
            tvTypeBadge.setText("OWE");
            tvTypeBadge.setTextColor(dangerColor);
            tvTypeBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(adjustAlpha(dangerColor, 0.15f)));
        } else {
            tvTypeBadge.setText("LENT");
            tvTypeBadge.setTextColor(successColor);
            tvTypeBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(adjustAlpha(successColor, 0.15f)));
        }

        if (currentItem.isSettled()) {
            tvStatusBadge.setText("SETTLED");
            tvStatusBadge.setTextColor(successColor);
            tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(adjustAlpha(successColor, 0.15f)));
            btnRecordPayment.setEnabled(false);
            btnSettleFull.setEnabled(false);
        } else {
            tvStatusBadge.setText("PENDING");
            tvStatusBadge.setTextColor(ThemeManager.getColor(this, ThemeManager.ThemeColor.WARNING));
            tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(adjustAlpha(ThemeManager.getColor(this, ThemeManager.ThemeColor.WARNING), 0.15f)));
            btnRecordPayment.setEnabled(true);
            btnSettleFull.setEnabled(true);
        }

        if (currentItem.getDueTimestamp() > 0) {
            tvDueDate.setVisibility(View.VISIBLE);
            tvDueDate.setText(String.format("Due Date: %s", dateFormat.format(new Date(currentItem.getDueTimestamp()))));
            if (!currentItem.isSettled() && currentItem.getDueTimestamp() < System.currentTimeMillis()) {
                tvDueDate.setTextColor(dangerColor);
            } else {
                tvDueDate.setTextColor(ThemeManager.getColor(this, ThemeManager.ThemeColor.TEXT_SECONDARY));
            }
        } else {
            tvDueDate.setVisibility(View.GONE);
        }
    }

    private void calculateSettlement(List<BorrowOwePayment> payments) {
        if (currentItem == null) return;

        double totalPaid = 0.0;
        for (BorrowOwePayment payment : payments) {
            totalPaid += payment.getAmountPaid();
        }

        double initial = currentItem.getAmount();
        currentRemaining = initial - totalPaid;
        if (currentRemaining < 0) currentRemaining = 0.0;

        tvPaidAmount.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, totalPaid));
        tvRemainingAmount.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, currentRemaining));

        int percentage = (int) Math.round((totalPaid / initial) * 100.0);
        if (percentage > 100) percentage = 100;
        
        progressSettlement.setProgress(percentage);
        tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", percentage));

        // Auto-settlement trigger
        if (currentRemaining <= 0.001 && !currentItem.isSettled()) {
            currentItem.setSettled(true);
            viewModel.updateBorrowOwe(currentItem);
        } else if (currentRemaining > 0.001 && currentItem.isSettled()) {
            currentItem.setSettled(false);
            viewModel.updateBorrowOwe(currentItem);
        }
    }

    private void showDeletePaymentConfirmation(BorrowOwePayment payment) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Payment Log")
                .setMessage("Are you sure you want to delete this payment log?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteBorrowOwePayment(payment);
                    Toast.makeText(this, "Payment log deleted", Toast.LENGTH_SHORT).show();

                    if (currentItem != null && !currentItem.isBorrow() && currentItem.isWasAddedAsExpense()) {
                        final double restoredAmount = payment.getAmountPaid();
                        com.example.expenseeye.database.AppDatabase.databaseWriteExecutor.execute(() -> {
                            com.example.expenseeye.models.Expense originalExpense = viewModel.findExpenseForBorrowOweSync(currentItem.getTimestamp());
                            if (originalExpense != null) {
                                runOnUiThread(() -> {
                                    double currentExpAmount = originalExpense.getAmount();
                                    double updatedExpAmount = currentExpAmount + restoredAmount;
                                    new MaterialAlertDialogBuilder(this)
                                            .setTitle("Restore Linked Expense")
                                            .setMessage("Since you deleted this payback payment, would you like to restore the expense amount back to " + currencySymbol + String.format(Locale.getDefault(), "%.2f", updatedExpAmount) + "?")
                                            .setPositiveButton("Restore Amount", (expDialog, expWhich) -> {
                                                originalExpense.setAmount(updatedExpAmount);
                                                viewModel.updateExpense(originalExpense);
                                                Toast.makeText(this, "Linked expense restored", Toast.LENGTH_SHORT).show();
                                            })
                                            .setNegativeButton("Keep As Is", null)
                                            .show();
                                });
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRecordPaymentDialog(double initialAmount) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_record_payment, null);

        EditText etAmount = view.findViewById(R.id.et_pay_amount);
        EditText etNote = view.findViewById(R.id.et_pay_note);
        MaterialSwitch switchLog = view.findViewById(R.id.switch_pay_log_as_expense);
        View layoutOptions = view.findViewById(R.id.layout_pay_expense_options);
        AutoCompleteTextView spinnerCategory = view.findViewById(R.id.spinner_pay_category);
        AutoCompleteTextView spinnerPayment = view.findViewById(R.id.spinner_pay_payment_method);

        // Percentage calculations
        MaterialButton btn10 = view.findViewById(R.id.btn_pct_10);
        MaterialButton btn25 = view.findViewById(R.id.btn_pct_25);
        MaterialButton btn50 = view.findViewById(R.id.btn_pct_50);
        MaterialButton btn100 = view.findViewById(R.id.btn_pct_100);

        btn10.setOnClickListener(v -> etAmount.setText(String.format(Locale.US, "%.2f", currentRemaining * 0.1)));
        btn25.setOnClickListener(v -> etAmount.setText(String.format(Locale.US, "%.2f", currentRemaining * 0.25)));
        btn50.setOnClickListener(v -> etAmount.setText(String.format(Locale.US, "%.2f", currentRemaining * 0.5)));
        btn100.setOnClickListener(v -> etAmount.setText(String.format(Locale.US, "%.2f", currentRemaining)));

        if (initialAmount > 0) {
            etAmount.setText(String.format(Locale.US, "%.2f", initialAmount));
        }

        // Spinners
        List<String> catNames = new ArrayList<>();
        for (Category c : availableCategories) catNames.add(c.getName());
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, catNames);
        spinnerCategory.setAdapter(catAdapter);

        List<String> pmNames = new ArrayList<>();
        for (PaymentMethod pm : availablePaymentMethods) pmNames.add(pm.getName());
        ArrayAdapter<String> pmAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, pmNames);
        spinnerPayment.setAdapter(pmAdapter);

        if (!availableCategories.isEmpty()) {
            spinnerCategory.setText(availableCategories.get(0).getName(), false);
        }
        int savedPaymentId = preferenceHelper.getDefaultPaymentMethodId();
        if (savedPaymentId != -1) {
            for (PaymentMethod pm : availablePaymentMethods) {
                if (pm.getId() == savedPaymentId) {
                    spinnerPayment.setText(pm.getName(), false);
                    break;
                }
            }
        } else if (!availablePaymentMethods.isEmpty()) {
            spinnerPayment.setText(availablePaymentMethods.get(0).getName(), false);
        }

        spinnerCategory.setOnClickListener(v -> spinnerCategory.showDropDown());
        spinnerPayment.setOnClickListener(v -> spinnerPayment.showDropDown());

        if (currentItem.isBorrow()) {
            switchLog.setVisibility(View.VISIBLE);
        } else {
            switchLog.setVisibility(View.GONE);
            layoutOptions.setVisibility(View.GONE);
        }

        switchLog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (switchLog.getVisibility() == View.VISIBLE) {
                layoutOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            } else {
                layoutOptions.setVisibility(View.GONE);
            }
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        view.findViewById(R.id.btn_pay_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_pay_save).setOnClickListener(v -> {
            String amtStr = etAmount.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (amtStr.isEmpty()) {
                etAmount.setError("Amount is required");
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amtStr);
                if (amount <= 0) {
                    etAmount.setError("Amount must be greater than zero");
                    return;
                }
                if (amount > currentRemaining + 0.01) {
                    etAmount.setError("Amount exceeds remaining debt of " + currencySymbol + currentRemaining);
                    return;
                }
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount");
                return;
            }

            // Save Payment Log
            BorrowOwePayment payment = new BorrowOwePayment(
                    borrowOweId,
                    amount,
                    System.currentTimeMillis(),
                    note
            );
            viewModel.insertBorrowOwePayment(payment);

            // Log corresponding Expense entry
            if (switchLog.isChecked()) {
                String selectedCatName = spinnerCategory.getText().toString();
                String selectedPmName = spinnerPayment.getText().toString();

                Category chosenCat = null;
                for (Category c : availableCategories) {
                    if (c.getName().equals(selectedCatName)) {
                        chosenCat = c;
                        break;
                    }
                }

                PaymentMethod chosenPm = null;
                for (PaymentMethod pm : availablePaymentMethods) {
                    if (pm.getName().equals(selectedPmName)) {
                        chosenPm = pm;
                        break;
                    }
                }

                int catId = chosenCat != null ? chosenCat.getId() : 1;
                String catName = chosenCat != null ? chosenCat.getName() : "Others";
                int pmId = chosenPm != null ? chosenPm.getId() : 1;
                String pmName = chosenPm != null ? chosenPm.getName() : "Cash";

                String expTitle = (currentItem.isBorrow() ? "Paid back " : "Received from ") + currentItem.getPersonName();
                if (!note.isEmpty()) {
                    expTitle += " (" + note + ")";
                }

                Expense exp = new Expense(
                        expTitle,
                        "Borrow/Owe Payment Reference",
                        amount,
                        System.currentTimeMillis(),
                        catId,
                        catName,
                        pmId,
                        pmName,
                        0
                );
                viewModel.insertExpense(exp);
            }

            Toast.makeText(this, "Payment recorded successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            final double payAmount = amount;
            if (currentItem != null && !currentItem.isBorrow() && currentItem.isWasAddedAsExpense()) {
                com.example.expenseeye.database.AppDatabase.databaseWriteExecutor.execute(() -> {
                    com.example.expenseeye.models.Expense originalExpense = viewModel.findExpenseForBorrowOweSync(currentItem.getTimestamp());
                    if (originalExpense != null) {
                        runOnUiThread(() -> showUpdateExpensePopup(originalExpense, payAmount));
                    }
                });
            }
        });

        dialog.show();
    }

    private void showUpdateExpensePopup(final Expense originalExpense, final double payAmount) {
        double currentExpAmount = originalExpense.getAmount();
        double calculatedAmount = currentExpAmount - payAmount;
        if (calculatedAmount < 0) {
            calculatedAmount = 0;
        }
        final double updatedExpAmount = calculatedAmount;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Update Linked Expense Log");

        if (updatedExpAmount <= 0.001) {
            builder.setMessage("This transaction was logged as an expense of " + currencySymbol + String.format(Locale.getDefault(), "%.2f", currentExpAmount) + ".\n\nSince it is now fully paid back, would you like to delete the corresponding expense log to avoid conflict?");
            builder.setPositiveButton("Delete Expense", (dialog, which) -> {
                viewModel.deleteExpense(originalExpense);
                Toast.makeText(this, "Linked expense log deleted", Toast.LENGTH_SHORT).show();
            });
        } else {
            builder.setMessage("This transaction was logged as an expense of " + currencySymbol + String.format(Locale.getDefault(), "%.2f", currentExpAmount) + ".\n\nSince " + currencySymbol + String.format(Locale.getDefault(), "%.2f", payAmount) + " is being paid back, would you like to update the expense to " + currencySymbol + String.format(Locale.getDefault(), "%.2f", updatedExpAmount) + "?");
            builder.setPositiveButton("Update Expense", (dialog, which) -> {
                originalExpense.setAmount(updatedExpAmount);
                viewModel.updateExpense(originalExpense);
                Toast.makeText(this, "Linked expense log updated to " + currencySymbol + String.format(Locale.getDefault(), "%.2f", updatedExpAmount), Toast.LENGTH_SHORT).show();
            });
            builder.setNeutralButton("Delete Instead", (dialog, which) -> {
                viewModel.deleteExpense(originalExpense);
                Toast.makeText(this, "Linked expense log deleted", Toast.LENGTH_SHORT).show();
            });
        }

        builder.setNegativeButton("Keep As Is", null);
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_borrow_owe_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            showEditDialog();
            return true;
        } else if (id == R.id.action_delete) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Record")
                    .setMessage("Are you sure you want to delete this borrow/owe record and all its payment history?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        final BorrowOwe itemToDelete = currentItem;
                        viewModel.deleteBorrowOwe(itemToDelete);
                        Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();

                        if (itemToDelete != null && !itemToDelete.isBorrow() && itemToDelete.isWasAddedAsExpense()) {
                            com.example.expenseeye.database.AppDatabase.databaseWriteExecutor.execute(() -> {
                                com.example.expenseeye.models.Expense originalExpense = viewModel.findExpenseForBorrowOweSync(itemToDelete.getTimestamp());
                                if (originalExpense != null) {
                                    runOnUiThread(() -> {
                                        new MaterialAlertDialogBuilder(this)
                                                .setTitle("Delete Linked Expense")
                                                .setMessage("Would you like to delete the corresponding expense log as well to avoid conflict?")
                                                .setPositiveButton("Delete Expense", (expDialog, expWhich) -> {
                                                    viewModel.deleteExpense(originalExpense);
                                                    Toast.makeText(this, "Linked expense log deleted", Toast.LENGTH_SHORT).show();
                                                    finish();
                                                })
                                                .setNegativeButton("Keep Expense", (expDialog, expWhich) -> finish())
                                                .setOnCancelListener(dialogInterface -> finish())
                                                .show();
                                    });
                                } else {
                                    runOnUiThread(this::finish);
                                }
                            });
                        } else {
                            finish();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showEditDialog() {
        // Reuse original add/edit form dialog by opening a Dialog
        // To do this simply, we copy the edit dialog setup from BorrowOweActivity
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_borrow_owe, null);

        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        EditText etName = view.findViewById(R.id.et_person_name);
        EditText etAmount = view.findViewById(R.id.et_amount);
        MaterialButtonToggleGroup toggleType = view.findViewById(R.id.toggle_type);
        EditText etDescription = view.findViewById(R.id.et_description);
        EditText etDueDate = view.findViewById(R.id.et_due_date);
        MaterialSwitch switchLogExpense = view.findViewById(R.id.switch_log_as_expense);
        View layoutExpenseOptions = view.findViewById(R.id.layout_expense_options);

        tvTitle.setText("Edit Transaction");
        etName.setText(currentItem.getPersonName());
        etAmount.setText(String.valueOf(currentItem.getAmount()));
        toggleType.check(currentItem.isBorrow() ? R.id.btn_type_borrowed : R.id.btn_type_lent);
        etDescription.setText(currentItem.getDescription());
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (currentItem.getDueTimestamp() > 0) {
            cal.setTimeInMillis(currentItem.getDueTimestamp());
            etDueDate.setText(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(cal.getTime()));
        } else {
            etDueDate.setText("");
        }

        switchLogExpense.setVisibility(View.GONE); // Cannot relog
        layoutExpenseOptions.setVisibility(View.GONE);

        etDueDate.setOnClickListener(v -> {
            new android.app.DatePickerDialog(this, (dView, year, month, dayOfMonth) -> {
                cal.set(java.util.Calendar.YEAR, year);
                cal.set(java.util.Calendar.MONTH, month);
                cal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
                etDueDate.setText(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(cal.getTime()));
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String amtStr = etAmount.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }
            if (amtStr.isEmpty()) {
                etAmount.setError("Amount is required");
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amtStr);
                if (amount <= 0) {
                    etAmount.setError("Amount must be greater than zero");
                    return;
                }
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount");
                return;
            }

            boolean isBorrow = (toggleType.getCheckedButtonId() == R.id.btn_type_borrowed);
            long dueTs = etDueDate.getText().toString().isEmpty() ? 0 : cal.getTimeInMillis();

            currentItem.setPersonName(name);
            currentItem.setAmount(amount);
            currentItem.setBorrow(isBorrow);
            currentItem.setDescription(desc);
            currentItem.setDueTimestamp(dueTs);

            viewModel.updateBorrowOwe(currentItem);
            Toast.makeText(this, "Record updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(android.graphics.Color.alpha(color) * factor);
        int red = android.graphics.Color.red(color);
        int green = android.graphics.Color.green(color);
        int blue = android.graphics.Color.blue(color);
        return android.graphics.Color.argb(alpha, red, green, blue);
    }
}
