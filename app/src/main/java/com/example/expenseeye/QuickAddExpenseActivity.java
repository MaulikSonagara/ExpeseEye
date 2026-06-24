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
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.expenseeye.adapters.PaymentMethodAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class QuickAddExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "extra_category";

    private AppRepository repository;
    private Calendar selectedDateTime = Calendar.getInstance();

    private RelativeLayout rootLayout;
    private CardView cardQuickAdd;
    private EditText etAmount, etDescription;
    private AutoCompleteTextView etTitle, spinnerCategory;
    private Button btnDate, btnTime, btnCancel, btnSave;

    private List<Category> availableCategories = new ArrayList<>();
    private List<PaymentMethod> availablePaymentMethods = new ArrayList<>();
    private List<String> categoryNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load and apply theme preference
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_add_expense);

        // Bind views
        rootLayout = findViewById(R.id.rl_quick_add_root);
        cardQuickAdd = findViewById(R.id.card_quick_add);
        etAmount = findViewById(R.id.et_amount);
        etTitle = findViewById(R.id.et_title);
        spinnerCategory = findViewById(R.id.spinner_category);
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
        final List<com.example.expenseeye.models.CategoryKeyword>[] allKeywords = new List[]{new ArrayList<>()};
        AppDatabase.databaseWriteExecutor.execute(() -> {
            availableCategories = repository.getEnabledCategoriesSync();
            availablePaymentMethods = repository.getAllPaymentMethodsSync();
            allKeywords[0] = repository.getAllKeywordsSync();

            // Populate category name list
            categoryNames.clear();
            for (Category cat : availableCategories) {
                categoryNames.add(cat.getName());
            }

            List<String> suggestions = new ArrayList<>();
            for (com.example.expenseeye.models.CategoryKeyword kw : allKeywords[0]) {
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

            runOnUiThread(() -> {
                setupCategorySpinner();
                setupPaymentMethodChips();
                // Check if category was preselected from widget
                handlePreselectedCategory();
                // Setup suggestions for title input
                setupTitleAutocomplete(suggestions);
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
                String titleText = s.toString();
                String descText = etDescription != null ? etDescription.getText().toString() : "";
                String classified = ExpenseClassifier.classifyExpense(titleText + " " + descText, availableCategories, allKeywords[0]);
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

    private void setupTitleAutocomplete(List<String> suggestions) {
        ArrayAdapter<String> titleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions);
        etTitle.setAdapter(titleAdapter);
    }

    private PaymentMethodAdapter adapterMain, adapterOther;

    private void setupPaymentMethodChips() {
        RecyclerView rvMain = findViewById(R.id.rv_payment_main);
        android.widget.LinearLayout layoutOther = findViewById(R.id.layout_payment_other);
        RecyclerView rvOther = findViewById(R.id.rv_payment_other);

        List<String> mainMethods = Arrays.asList("Cash", "UPI", "Other");
        List<String> otherMethods = Arrays.asList("Debit Card", "Credit Card", "Bank Transfer", "Wallet");

        adapterMain = new PaymentMethodAdapter(mainMethods, "UPI", name -> {
            if ("Other".equals(name)) {
                layoutOther.setVisibility(View.VISIBLE);
            } else {
                layoutOther.setVisibility(View.GONE);
                if (adapterOther != null) adapterOther.setSelectedMethod(null);
            }
        });

        rvMain.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMain.setAdapter(adapterMain);

        adapterOther = new PaymentMethodAdapter(otherMethods, "", name -> {
            // Handled internally by adapter
        });
        rvOther.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvOther.setAdapter(adapterOther);
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
        if (adapterMain != null) {
            String main = adapterMain.getSelectedMethod();
            if ("Other".equals(main) && adapterOther != null) {
                String other = adapterOther.getSelectedMethod();
                payment = (other != null) ? other : "Other";
            } else {
                payment = (main != null) ? main : "Cash";
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
