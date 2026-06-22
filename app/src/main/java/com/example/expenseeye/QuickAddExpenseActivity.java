package com.example.expenseeye;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.repository.AppRepository;
import com.example.expenseeye.utils.ExpenseClassifier;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class QuickAddExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "extra_category";

    private AppRepository repository;
    private Calendar selectedDateTime = Calendar.getInstance();

    private RelativeLayout rootLayout;
    private CardView cardQuickAdd;
    private EditText etAmount, etTitle, etDescription;
    private AutoCompleteTextView spinnerCategory;
    private ChipGroup cgPaymentMethod;
    private android.widget.LinearLayout layoutCardType;
    private ChipGroup cgCardType;
    private Chip chipDebitCard, chipCreditCard;
    private Button btnDate, btnTime, btnCancel, btnSave;

    private List<Category> availableCategories = new ArrayList<>();
    private List<PaymentMethod> availablePaymentMethods = new ArrayList<>();
    private List<String> categoryNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load and apply theme preference
        SharedPreferences sharedPrefs = getSharedPreferences("ExpenseEyePrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPrefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_add_expense);

        // Bind views
        rootLayout = findViewById(R.id.rl_quick_add_root);
        cardQuickAdd = findViewById(R.id.card_quick_add);
        etAmount = findViewById(R.id.et_amount);
        etTitle = findViewById(R.id.et_title);
        spinnerCategory = findViewById(R.id.spinner_category);
        cgPaymentMethod = findViewById(R.id.cg_payment_method);
        layoutCardType = findViewById(R.id.layout_card_type);
        cgCardType = findViewById(R.id.cg_card_type);
        chipDebitCard = findViewById(R.id.chip_debit_card);
        chipCreditCard = findViewById(R.id.chip_credit_card);
        btnDate = findViewById(R.id.btn_date);
        btnTime = findViewById(R.id.btn_time);
        etDescription = findViewById(R.id.et_description);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);

        // Initialize Repository
        repository = new AppRepository(getApplication());

        // Initial setup
        setupDateTimeButtons();

        // Load Categories & Payment Methods in background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            availableCategories = repository.getAllCategoriesSync();
            availablePaymentMethods = repository.getAllPaymentMethodsSync();

            // Populate category name list
            categoryNames.clear();
            for (Category cat : availableCategories) {
                categoryNames.add(cat.getName());
            }

            runOnUiThread(() -> {
                setupCategorySpinner();
                setupPaymentMethodChips();
                // Check if category was preselected from widget
                handlePreselectedCategory();
            });
        });

        // Smart Classifier Text Watcher
        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String classified = ExpenseClassifier.classifyExpense(s.toString());
                int index = categoryNames.indexOf(classified);
                if (index >= 0) {
                    spinnerCategory.setText(classified, false);
                }
            }
        });

        // Cancel click listener
        btnCancel.setOnClickListener(v -> dismissWithAnimation());

        // Root layout tap (dismiss when click outside dialog card)
        rootLayout.setOnClickListener(v -> dismissWithAnimation());
        cardQuickAdd.setOnClickListener(v -> {
            // Prevent clicks inside card from closing activity
        });

        // Save click listener
        btnSave.setOnClickListener(v -> saveExpense());

        // Animate views entrance
        animateEntrance();
    }

    private void setupDateTimeButtons() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        btnDate.setText(sdfDate.format(selectedDateTime.getTime()));
        btnTime.setText(sdfTime.format(selectedDateTime.getTime()));

        btnDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                btnDate.setText(sdfDate.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        btnTime.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                btnTime.setText(sdfTime.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), false);
            timePicker.show();
        });
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryNames);
        spinnerCategory.setAdapter(catAdapter);
    }

    private void setupPaymentMethodChips() {
        cgPaymentMethod.removeAllViews();
        String[] mainMethods = {"UPI", "Cash", "Debit/Credit", "Bank Transfer", "Other"};
        
        for (String name : mainMethods) {
            Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
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
            chip.setChipIcon(androidx.core.content.ContextCompat.getDrawable(this, iconResId));
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

            // Default select UPI
            if (name.equals("UPI")) {
                chip.setChecked(true);
            }
            cgPaymentMethod.addView(chip);
        }
    }

    private void handlePreselectedCategory() {
        String preSelected = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (preSelected != null && !preSelected.isEmpty()) {
            // Find index of the preselected category name
            int index = -1;
            for (int i = 0; i < categoryNames.size(); i++) {
                if (categoryNames.get(i).equalsIgnoreCase(preSelected)) {
                    index = i;
                    break;
                }
            }
            // If category is not in DB but is Food/Travel/Shopping, map them to database standard categories
            if (index == -1) {
                if (preSelected.equalsIgnoreCase("Food")) {
                    index = categoryNames.indexOf("Groceries");
                } else if (preSelected.equalsIgnoreCase("Travel")) {
                    index = categoryNames.indexOf("Transport");
                } else if (preSelected.equalsIgnoreCase("Shop") || preSelected.equalsIgnoreCase("Shopping")) {
                    index = categoryNames.indexOf("Shopping");
                }
            }

            if (index >= 0) {
                spinnerCategory.setText(categoryNames.get(index), false);
            }
        }
    }

    private void animateEntrance() {
        rootLayout.setAlpha(0f);
        rootLayout.animate().alpha(1f).setDuration(250).start();

        cardQuickAdd.post(() -> {
            cardQuickAdd.setScaleX(0.8f);
            cardQuickAdd.setScaleY(0.8f);
            cardQuickAdd.setAlpha(0f);
            cardQuickAdd.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .withEndAction(() -> {
                        // Focus on amount and show keyboard
                        etAmount.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(etAmount, InputMethodManager.SHOW_IMPLICIT);
                        }
                    })
                    .start();
        });
    }

    private void dismissWithAnimation() {
        // Hide soft keyboard
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

        rootLayout.animate().alpha(0f).setDuration(200).start();
        cardQuickAdd.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(this::finish)
                .start();
    }

    private void saveExpense() {
        String amountStr = etAmount.getText().toString().trim();
        String titleStr = etTitle.getText().toString().trim();

        if (amountStr.isEmpty() || titleStr.isEmpty()) {
            Toast.makeText(this, "Please enter both amount and title", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String category = spinnerCategory.getText() != null ? spinnerCategory.getText().toString() : "Other";

        String payment = "Other";
        int checkedChipId = cgPaymentMethod.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip checkedChip = findViewById(checkedChipId);
            if (checkedChip != null) {
                String checkedText = checkedChip.getText().toString();
                if (checkedText.equals("Debit/Credit")) {
                    int cardCheckedId = cgCardType.getCheckedChipId();
                    if (cardCheckedId == R.id.chip_credit_card) {
                        payment = "Credit Card";
                    } else {
                        payment = "Debit Card";
                    }
                } else {
                    payment = checkedText;
                }
            }
        }

        String desc = etDescription.getText().toString().trim();

        // Match category details
        int catId = 0;
        for (Category c : availableCategories) {
            if (c.getName().equals(category)) {
                catId = c.getId();
                break;
            }
        }

        // Match payment method details
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

        // Save to DB in background
        AppDatabase.databaseWriteExecutor.execute(() -> {
            repository.insertExpense(newExpense);
            runOnUiThread(() -> {
                Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show();
                dismissWithAnimation();
            });
        });
    }

    @Override
    public void onBackPressed() {
        dismissWithAnimation();
    }
}
