package com.example.expenseeye;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.adapters.BorrowOweAdapter;
import com.example.expenseeye.models.BorrowOwe;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.theme.ThemePreferenceHelper;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BorrowOweActivity extends AppCompatActivity {

    private AppViewModel viewModel;
    private ThemePreferenceHelper preferenceHelper;
    private BorrowOweAdapter adapter;
    
    private TextView tvTotalOwe, tvTotalOwed, tvEmptyState;
    private TabLayout tabLayout;
    private RecyclerView rvBorrowOwe;
    
    private List<BorrowOwe> allItems = new ArrayList<>();
    private List<Category> availableCategories = new ArrayList<>();
    private List<PaymentMethod> availablePaymentMethods = new ArrayList<>();
    
    private String currencySymbol = "₹";
    private Calendar dialogDueCalendar = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private EditText activeNameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_borrow_owe);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        preferenceHelper = new ThemePreferenceHelper(this);
        currencySymbol = preferenceHelper.getCurrencySymbol();

        tvTotalOwe = findViewById(R.id.tv_total_owe);
        tvTotalOwed = findViewById(R.id.tv_total_owed);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        tabLayout = findViewById(R.id.tab_layout);
        rvBorrowOwe = findViewById(R.id.rv_borrow_owe);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);

        rvBorrowOwe.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BorrowOweAdapter(currencySymbol, new BorrowOweAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BorrowOwe item) {
                Intent intent = new Intent(BorrowOweActivity.this, BorrowOweDetailsActivity.class);
                intent.putExtra("borrow_owe_id", item.getId());
                startActivity(intent);
            }

            @Override
            public void onSettleClick(BorrowOwe item) {
                showSettleConfirmation(item);
            }
        });
        rvBorrowOwe.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        // Observe stats
        viewModel.getTotalOwedToOthers().observe(this, owe -> {
            double val = owe != null ? owe : 0.0;
            tvTotalOwe.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, val));
        });

        viewModel.getTotalOwedToMe().observe(this, owed -> {
            double val = owed != null ? owed : 0.0;
            tvTotalOwed.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, val));
        });

        // Observe data
        viewModel.getAllBorrowOwes().observe(this, items -> {
            if (items != null) {
                allItems = items;
                filterAndSubmitList();
            }
        });

        // Cache category and payment method lists
        viewModel.getEnabledCategories().observe(this, cats -> {
            if (cats != null) availableCategories = cats;
        });

        viewModel.getAllPaymentMethods().observe(this, pms -> {
            if (pms != null) availablePaymentMethods = pms;
        });

        // Tab selection
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterAndSubmitList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        fabAdd.setOnClickListener(v -> showAddEditDialog(null));
    }

    private void filterAndSubmitList() {
        int selectedTab = tabLayout.getSelectedTabPosition();
        boolean filterSettled = (selectedTab == 1); // Tab 0 = Pending, Tab 1 = Settled

        List<BorrowOwe> filtered = new ArrayList<>();
        for (BorrowOwe item : allItems) {
            if (item.isSettled() == filterSettled) {
                filtered.add(item);
            }
        }

        adapter.submitList(filtered);

        if (filtered.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(filterSettled ? "No settled transactions." : "No pending transactions.");
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showSettleConfirmation(BorrowOwe item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Mark as Settled")
                .setMessage("Are you sure you want to mark this transaction as settled?")
                .setPositiveButton("Settle", (dialog, which) -> {
                    item.setSettled(true);
                    viewModel.updateBorrowOwe(item);
                    Toast.makeText(this, "Transaction settled!", Toast.LENGTH_SHORT).show();

                    if (!item.isBorrow() && item.isWasAddedAsExpense()) {
                        com.example.expenseeye.database.AppDatabase.databaseWriteExecutor.execute(() -> {
                            com.example.expenseeye.models.Expense originalExpense = viewModel.findExpenseForBorrowOweSync(item.getTimestamp());
                            if (originalExpense != null) {
                                runOnUiThread(() -> {
                                    new MaterialAlertDialogBuilder(this)
                                            .setTitle("Delete Linked Expense")
                                            .setMessage("Since this lent transaction is now settled, would you like to delete the corresponding expense log to avoid conflict?")
                                            .setPositiveButton("Delete Expense", (expDialog, expWhich) -> {
                                                viewModel.deleteExpense(originalExpense);
                                                Toast.makeText(this, "Linked expense log deleted", Toast.LENGTH_SHORT).show();
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

    private void launchContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, 102);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchContactPicker();
        } else {
            Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 102 && resultCode == RESULT_OK && data != null) {
            android.net.Uri contactUri = data.getData();
            String[] projection = new String[]{android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
            try (android.database.Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        String name = cursor.getString(nameIndex);
                        if (activeNameInput != null) {
                            activeNameInput.setText(name);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showAddEditDialog(BorrowOwe itemToEdit) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_borrow_owe, null);
        
        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        com.google.android.material.textfield.TextInputLayout tilPersonName = view.findViewById(R.id.til_person_name);
        EditText etName = view.findViewById(R.id.et_person_name);
        EditText etAmount = view.findViewById(R.id.et_amount);
        MaterialButtonToggleGroup toggleType = view.findViewById(R.id.toggle_type);
        EditText etDescription = view.findViewById(R.id.et_description);
        EditText etDueDate = view.findViewById(R.id.et_due_date);
        MaterialSwitch switchLogExpense = view.findViewById(R.id.switch_log_as_expense);
        View layoutExpenseOptions = view.findViewById(R.id.layout_expense_options);
        AutoCompleteTextView spinnerCategory = view.findViewById(R.id.spinner_category);
        AutoCompleteTextView spinnerPayment = view.findViewById(R.id.spinner_payment_method);

        activeNameInput = etName;

        tilPersonName.setEndIconOnClickListener(v -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_CONTACTS}, 101);
            } else {
                launchContactPicker();
            }
        });

        // Prepopulate Categories Spinner
        List<String> catNames = new ArrayList<>();
        for (Category c : availableCategories) catNames.add(c.getName());
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, catNames);
        spinnerCategory.setAdapter(catAdapter);

        // Prepopulate Payment Spinner
        List<String> pmNames = new ArrayList<>();
        for (PaymentMethod pm : availablePaymentMethods) pmNames.add(pm.getName());
        ArrayAdapter<String> pmAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, pmNames);
        spinnerPayment.setAdapter(pmAdapter);

        // Set default category and payment method values
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

        dialogDueCalendar = Calendar.getInstance();
        boolean isEdit = (itemToEdit != null);
        
        if (isEdit) {
            tvTitle.setText("Edit Transaction");
            etName.setText(itemToEdit.getPersonName());
            etAmount.setText(String.valueOf(itemToEdit.getAmount()));
            toggleType.check(itemToEdit.isBorrow() ? R.id.btn_type_borrowed : R.id.btn_type_lent);
            etDescription.setText(itemToEdit.getDescription());
            if (itemToEdit.getDueTimestamp() > 0) {
                dialogDueCalendar.setTimeInMillis(itemToEdit.getDueTimestamp());
                etDueDate.setText(dateFormat.format(dialogDueCalendar.getTime()));
            } else {
                etDueDate.setText("");
            }
            // Cannot log again as expense upon edit
            switchLogExpense.setVisibility(View.GONE);
            layoutExpenseOptions.setVisibility(View.GONE);
        } else {
            tvTitle.setText("Add Transaction");
            toggleType.check(R.id.btn_type_borrowed);
            etDueDate.setText("");
            switchLogExpense.setVisibility(View.GONE);
            switchLogExpense.setChecked(false);
            layoutExpenseOptions.setVisibility(View.GONE);
        }

        // Toggle Switch visibility based on Borrowed vs Lent type
        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_type_borrowed) {
                    switchLogExpense.setChecked(false);
                    switchLogExpense.setVisibility(View.GONE);
                    layoutExpenseOptions.setVisibility(View.GONE);
                } else if (checkedId == R.id.btn_type_lent) {
                    if (!isEdit) {
                        switchLogExpense.setVisibility(View.VISIBLE);
                        if (switchLogExpense.isChecked()) {
                            layoutExpenseOptions.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });

        // Dropdown popups
        spinnerCategory.setOnClickListener(v -> spinnerCategory.showDropDown());
        spinnerPayment.setOnClickListener(v -> spinnerPayment.showDropDown());

        // Toggle Switch visibility for expandable options
        switchLogExpense.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (switchLogExpense.getVisibility() == View.VISIBLE) {
                layoutExpenseOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            } else {
                layoutExpenseOptions.setVisibility(View.GONE);
            }
        });

        // Setup DatePicker click on input field
        View.OnClickListener datePickerListener = v -> {
            DatePickerDialog dpd = new DatePickerDialog(this, (dView, year, month, dayOfMonth) -> {
                dialogDueCalendar.set(Calendar.YEAR, year);
                dialogDueCalendar.set(Calendar.MONTH, month);
                dialogDueCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                dialogDueCalendar.set(Calendar.HOUR_OF_DAY, 23);
                dialogDueCalendar.set(Calendar.MINUTE, 59);
                dialogDueCalendar.set(Calendar.SECOND, 59);
                dialogDueCalendar.set(Calendar.MILLISECOND, 999);
                etDueDate.setText(dateFormat.format(dialogDueCalendar.getTime()));
            }, dialogDueCalendar.get(Calendar.YEAR), dialogDueCalendar.get(Calendar.MONTH), dialogDueCalendar.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        };
        etDueDate.setOnClickListener(datePickerListener);

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
            long dueTs = etDueDate.getText().toString().isEmpty() ? 0 : dialogDueCalendar.getTimeInMillis();

            if (isEdit) {
                itemToEdit.setPersonName(name);
                itemToEdit.setAmount(amount);
                itemToEdit.setBorrow(isBorrow);
                itemToEdit.setDescription(desc);
                itemToEdit.setDueTimestamp(dueTs);
                viewModel.updateBorrowOwe(itemToEdit);
                Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show();
            } else {
                boolean logExpense = switchLogExpense.isChecked();
                
                BorrowOwe newItem = new BorrowOwe(
                        name,
                        amount,
                        isBorrow,
                        desc,
                        System.currentTimeMillis(),
                        dueTs,
                        false,
                        logExpense
                );
                viewModel.insertBorrowOwe(newItem);

                if (logExpense) {
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

                    String expTitle = (isBorrow ? "Borrowed from " : "Lent to ") + name;
                    if (!desc.isEmpty()) {
                        expTitle += " (" + desc + ")";
                    }

                    // Create corresponding expense log entry (type 0 = Expense)
                    Expense exp = new Expense(
                            expTitle,
                            "Borrow/Owe Reference",
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
                
                Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        dialog.show();
    }
}
