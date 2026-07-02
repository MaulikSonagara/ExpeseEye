package com.example.expenseeye.utils;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.adapters.PaymentMethodAdapter;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.CategoryKeyword;
import com.example.expenseeye.models.ChecklistItem;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.theme.ThemePreferenceHelper;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExpenseDialogHelper {

    public interface DialogCallback {
        void onDismiss();
    }

    public static void showExpenseDialog(Context context, LayoutInflater inflater, AppViewModel viewModel, 
                                         Expense expenseToEdit, ChecklistItem checklistItem, DialogCallback callback) {
        // Run database queries on a background thread to prevent blocking the main thread
        com.example.expenseeye.database.AppDatabase.databaseWriteExecutor.execute(() -> {
            final List<Category> allCategories = viewModel.getAllCategoriesSync();
            List<Category> tempEnabled = viewModel.getEnabledCategoriesSync();
            final List<Category> enabledCategories = (tempEnabled == null || tempEnabled.isEmpty()) ? allCategories : tempEnabled;
            final List<PaymentMethod> allPaymentMethods = viewModel.getAllPaymentMethodsSync();
            final List<CategoryKeyword> allKeywords = viewModel.getAllKeywordsSync();
            final List<Expense> allExpenses = viewModel.getAllExpensesSync();

            // Post back to the main thread to show the Dialog UI
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                showExpenseDialogUi(context, inflater, viewModel, expenseToEdit, checklistItem, callback,
                        allCategories, enabledCategories, allPaymentMethods, allKeywords, allExpenses);
            });
        });
    }

    private static void showExpenseDialogUi(Context context, LayoutInflater inflater, AppViewModel viewModel, 
                                            Expense expenseToEdit, ChecklistItem checklistItem, DialogCallback callback,
                                            List<Category> allCategories, List<Category> enabledCategories,
                                            List<PaymentMethod> allPaymentMethods, List<CategoryKeyword> allKeywords,
                                            List<Expense> allExpenses) {
        Dialog dialog = new Dialog(context, R.style.Theme_ExpenseEye_Dialog);
        View dialogView = inflater.inflate(R.layout.dialog_add_expense, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        KeyboardFollow.attach(
                dialogView.findViewById(R.id.rl_quick_add_root),
                dialogView.findViewById(R.id.card_quick_add)
        );

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        AutoCompleteTextView etTitleInput = dialogView.findViewById(R.id.et_title);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        
        Button btnDate = dialogView.findViewById(R.id.btn_date);
        Button btnTime = dialogView.findViewById(R.id.btn_time);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete);

        // Keyword suggestion views
        MaterialCardView cardKeywordSuggestion = dialogView.findViewById(R.id.card_keyword_suggestion);
        TextView tvSuggestionText = dialogView.findViewById(R.id.tv_suggestion_text);
        Button btnAcceptSuggestion = dialogView.findViewById(R.id.btn_accept_suggestion);
        ImageButton btnDismissSuggestion = dialogView.findViewById(R.id.btn_dismiss_suggestion);

        // Category Buttons
        MaterialButton btnCat1 = dialogView.findViewById(R.id.btn_cat_1);
        MaterialButton btnCat2 = dialogView.findViewById(R.id.btn_cat_2);
        MaterialButton btnCat3 = dialogView.findViewById(R.id.btn_cat_3);
        MaterialButton btnCatMore = dialogView.findViewById(R.id.btn_cat_more);

        // Keep track of state
        final Calendar selectedDateTime = Calendar.getInstance();
        final Set<String> dismissedKeywords = new HashSet<>();
        final String[] currentCategory = { "Other" };



        // Calculate dynamic category frequencies
        List<String> top3Cats = getFrequentCategories(allExpenses, enabledCategories);
        if (top3Cats.size() > 0) btnCat1.setText(top3Cats.get(0));
        if (top3Cats.size() > 1) btnCat2.setText(top3Cats.get(1));
        if (top3Cats.size() > 2) btnCat3.setText(top3Cats.get(2));

        // Helper to update selection
        java.util.function.Consumer<String> selectCategory = (selectedCat) -> {
            currentCategory[0] = selectedCat;
            int colorSelectedBg = com.example.expenseeye.theme.ThemeManager.getColor(context, com.example.expenseeye.theme.ThemeManager.ThemeColor.PRIMARY);
            int colorSelectedText = com.example.expenseeye.theme.ThemeManager.getColor(context, com.example.expenseeye.theme.ThemeManager.ThemeColor.SURFACE);
            int colorUnselectedBg = Color.TRANSPARENT;
            int colorUnselectedText = com.example.expenseeye.theme.ThemeManager.getColor(context, com.example.expenseeye.theme.ThemeManager.ThemeColor.PRIMARY);
            int strokeColor = com.example.expenseeye.theme.ThemeManager.getColor(context, com.example.expenseeye.theme.ThemeManager.ThemeColor.DIVIDER);
            float density = context.getResources().getDisplayMetrics().density;
            int strokeWidth = (int) (1 * density);

            MaterialButton[] buttons = {btnCat1, btnCat2, btnCat3};
            boolean foundInTop3 = false;
            for (MaterialButton btn : buttons) {
                if (btn == null) continue;
                String btnText = btn.getText().toString();
                if (btnText.equalsIgnoreCase(selectedCat)) {
                    btn.setBackgroundTintList(ColorStateList.valueOf(colorSelectedBg));
                    btn.setTextColor(colorSelectedText);
                    btn.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
                    btn.setStrokeWidth(0);
                    foundInTop3 = true;
                } else {
                    btn.setBackgroundTintList(ColorStateList.valueOf(colorUnselectedBg));
                    btn.setTextColor(colorUnselectedText);
                    btn.setStrokeColor(ColorStateList.valueOf(strokeColor));
                    btn.setStrokeWidth(strokeWidth);
                }
            }

            if (foundInTop3) {
                btnCatMore.setText("More");
                btnCatMore.setBackgroundTintList(ColorStateList.valueOf(colorUnselectedBg));
                btnCatMore.setTextColor(colorUnselectedText);
                btnCatMore.setStrokeColor(ColorStateList.valueOf(strokeColor));
                btnCatMore.setStrokeWidth(strokeWidth);
            } else {
                btnCatMore.setText(selectedCat);
                btnCatMore.setBackgroundTintList(ColorStateList.valueOf(colorSelectedBg));
                btnCatMore.setTextColor(colorSelectedText);
                btnCatMore.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
                btnCatMore.setStrokeWidth(0);
            }
        };

        // Set Click Listeners for Category Buttons
        btnCat1.setOnClickListener(v -> selectCategory.accept(btnCat1.getText().toString()));
        btnCat2.setOnClickListener(v -> selectCategory.accept(btnCat2.getText().toString()));
        btnCat3.setOnClickListener(v -> selectCategory.accept(btnCat3.getText().toString()));

        List<Category> finalEnabledCategories = enabledCategories;
        btnCatMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, btnCatMore);
            for (Category cat : finalEnabledCategories) {
                popupMenu.getMenu().add(cat.getName());
            }
            popupMenu.setOnMenuItemClickListener(item -> {
                selectCategory.accept(item.getTitle().toString());
                return true;
            });
            popupMenu.show();
        });

        // Setup Title Suggestions autocomplete
        ThemePreferenceHelper prefHelper = new ThemePreferenceHelper(context);
        final String currencySymbol = prefHelper.getCurrencySymbol();
        etAmount.setHint(currencySymbol + "0.00");
        etAmount.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789." + currencySymbol));
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().startsWith(currencySymbol)) {
                    etAmount.removeTextChangedListener(this);
                    String cleanString = s.toString().replaceAll("[^0-9.]", "");
                    String newText = currencySymbol + cleanString;
                    etAmount.setText(newText);
                    etAmount.setSelection(newText.length());
                    etAmount.addTextChangedListener(this);
                }
            }
        });

        // Initialize with currency symbol for new expense
        if (expenseToEdit == null) {
            etAmount.setText(currencySymbol);
        }

        if (prefHelper.isTitleSuggestionsEnabled()) {
            List<String> suggestions = new ArrayList<>();
            for (CategoryKeyword kw : allKeywords) {
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
            ArrayAdapter<String> titleAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, suggestions);
            etTitleInput.setAdapter(titleAdapter);
        } else {
            etTitleInput.setAdapter(null);
        }

        // Setup Payment Method chips
        String initialPaymentMethod = "UPI";
        int savedPmId = prefHelper.getDefaultPaymentMethodId();
        if (savedPmId != -1) {
            for (PaymentMethod pm : allPaymentMethods) {
                if (pm.getId() == savedPmId) {
                    initialPaymentMethod = pm.getName();
                    break;
                }
            }
        }

        // Populate values based on Mode (Add, Edit, checklist item)
        if (expenseToEdit != null) {
            if (tvTitle != null) tvTitle.setText("Edit Expense");
            etAmount.setText(currencySymbol + expenseToEdit.getAmount());
            etTitleInput.setText(expenseToEdit.getTitle());
            etDescription.setText(expenseToEdit.getDescription());
            selectedDateTime.setTimeInMillis(expenseToEdit.getTimestamp());
            initialPaymentMethod = expenseToEdit.getPaymentMethodName();
            selectCategory.accept(expenseToEdit.getCategoryName());

            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                viewModel.deleteExpense(expenseToEdit);
                Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (callback != null) callback.onDismiss();
            });
        } else {
            if (checklistItem != null) {
                if (tvTitle != null) tvTitle.setText("Log Checklist Item as Expense");
                etTitleInput.setText(checklistItem.getTitle());
                etDescription.setText(checklistItem.getQuantity() != null && !checklistItem.getQuantity().isEmpty() ? "Qty: " + checklistItem.getQuantity() : "");
                String initialCat = checklistItem.getCategory();
                if (initialCat == null || initialCat.trim().isEmpty()) {
                    initialCat = ExpenseClassifier.classifyExpense(checklistItem.getTitle(), enabledCategories, allKeywords);
                }
                selectCategory.accept(initialCat);
            } else {
                if (tvTitle != null) tvTitle.setText("Add Expense");
                selectCategory.accept(top3Cats.isEmpty() ? "Other" : top3Cats.get(0));
            }
        }

        setupPaymentMethodChipsForDialog(context, dialogView, allPaymentMethods, initialPaymentMethod);

        // Smart Classifier Text Watcher & Keyword Suggestion Watcher
        List<CategoryKeyword> finalAllKeywords = allKeywords;
        List<Category> finalEnabledCategories1 = enabledCategories;
        
        etTitleInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String titleText = s.toString().trim();
                
                // Smart classifier
                if (prefHelper.isSmartClassifierEnabled() && !titleText.isEmpty()) {
                    String descText = etDescription.getText().toString();
                    String classified = ExpenseClassifier.classifyExpense(titleText + " " + descText, finalEnabledCategories1, finalAllKeywords);
                    if (classified != null && !classified.equals("Other")) {
                        selectCategory.accept(classified);
                    }
                }

                // Keyword suggestion banner
                if (titleText.length() >= 3 && !titleText.matches("\\d+") && !dismissedKeywords.contains(titleText.toLowerCase())) {
                    boolean keywordExists = false;
                    String cleanLower = titleText.toLowerCase();
                    for (CategoryKeyword kw : finalAllKeywords) {
                        if (kw.getKeyword() != null && kw.getKeyword().toLowerCase().trim().equals(cleanLower)) {
                            keywordExists = true;
                            break;
                        }
                    }
                    for (Category c : finalEnabledCategories1) {
                        if (c.getName().toLowerCase().trim().equals(cleanLower)) {
                            keywordExists = true;
                            break;
                        }
                    }

                    if (!keywordExists) {
                        tvSuggestionText.setText("Add '" + titleText + "' as category keyword?");
                        cardKeywordSuggestion.setVisibility(View.VISIBLE);
                        
                        btnAcceptSuggestion.setOnClickListener(v -> {
                            cardKeywordSuggestion.setVisibility(View.GONE);
                            showAddKeywordPopup(context, viewModel, titleText, finalEnabledCategories1, selectCategory);
                        });
                    } else {
                        cardKeywordSuggestion.setVisibility(View.GONE);
                    }
                } else {
                    cardKeywordSuggestion.setVisibility(View.GONE);
                }
            }
        });

        btnDismissSuggestion.setOnClickListener(v -> {
            String titleText = etTitleInput.getText().toString().trim();
            if (!titleText.isEmpty()) {
                dismissedKeywords.add(titleText.toLowerCase());
            }
            cardKeywordSuggestion.setVisibility(View.GONE);
        });

        // Setup Date & Time formatters and pickers
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        btnDate.setText(sdfDate.format(selectedDateTime.getTime()));
        btnTime.setText(sdfTime.format(selectedDateTime.getTime()));

        btnDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                btnDate.setText(sdfDate.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        btnTime.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(context, (view, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                btnTime.setText(sdfTime.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), false);
            timePicker.show();
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onDismiss();
        });

        List<Category> finalAllCategories = allCategories;
        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            amountStr = amountStr.replace(currencySymbol, "").replace(",", "").trim();
            String titleStr = etTitleInput.getText().toString().trim();
            if (amountStr.isEmpty() || titleStr.isEmpty()) {
                Toast.makeText(context, "Please enter both amount and title", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            String category = currentCategory[0];
            String payment = getSelectedPaymentMethodFromDialog(dialogView);
            String desc = etDescription.getText().toString().trim();

            int catId = 0;
            for (Category c : finalAllCategories) {
                if (c.getName().equalsIgnoreCase(category)) {
                    catId = c.getId();
                    break;
                }
            }

            int pmId = 0;
            for (PaymentMethod pm : allPaymentMethods) {
                if (pm.getName().equalsIgnoreCase(payment)) {
                    pmId = pm.getId();
                    break;
                }
            }

            Expense expense;
            if (expenseToEdit != null) {
                expense = new Expense(
                        titleStr, desc, amount, selectedDateTime.getTimeInMillis(),
                        catId, category, pmId, payment, expenseToEdit.getType()
                );
                expense.setId(expenseToEdit.getId());
                viewModel.updateExpense(expense);
                Toast.makeText(context, "Expense updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                expense = new Expense(
                        titleStr, desc, amount, selectedDateTime.getTimeInMillis(),
                        catId, category, pmId, payment, 0
                );
                viewModel.insertExpense(expense);
                Toast.makeText(context, "Expense added successfully", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
            if (callback != null) callback.onDismiss();
        });

        dialog.show();

        // Keyboard focus
        etAmount.requestFocus();
        etAmount.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etAmount, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    public static List<String> getFrequentCategories(List<Expense> expenses, List<Category> allCategories) {
        Map<String, Integer> freqMap = new HashMap<>();
        if (expenses != null) {
            for (Expense e : expenses) {
                String catName = e.getCategoryName();
                if (catName != null) {
                    freqMap.put(catName, freqMap.getOrDefault(catName, 0) + 1);
                }
            }
        }

        List<String> defaults = Arrays.asList("Food", "Travel", "Groceries");
        List<String> enabledCatNames = new ArrayList<>();
        for (Category c : allCategories) {
            if (c.isEnabled()) {
                enabledCatNames.add(c.getName());
            }
        }

        List<String> sortedCats = new ArrayList<>(enabledCatNames);
        Collections.sort(sortedCats, (c1, c2) -> {
            int f1 = freqMap.getOrDefault(c1, 0);
            int f2 = freqMap.getOrDefault(c2, 0);
            if (f1 != f2) {
                return Integer.compare(f2, f1);
            }
            int idx1 = defaults.indexOf(c1);
            int idx2 = defaults.indexOf(c2);
            if (idx1 != -1 && idx2 != -1) {
                return Integer.compare(idx1, idx2);
            } else if (idx1 != -1) {
                return -1;
            } else if (idx2 != -1) {
                return 1;
            }
            return c1.compareToIgnoreCase(c2);
        });

        List<String> top3 = new ArrayList<>();
        for (String cat : sortedCats) {
            if (top3.size() < 3) {
                top3.add(cat);
            } else {
                break;
            }
        }

        for (String def : defaults) {
            if (top3.size() >= 3) break;
            for (String enabled : enabledCatNames) {
                if (enabled.equalsIgnoreCase(def) && !top3.contains(enabled)) {
                    top3.add(enabled);
                    if (top3.size() >= 3) break;
                }
            }
        }

        for (String enabled : enabledCatNames) {
            if (top3.size() >= 3) break;
            if (!top3.contains(enabled)) {
                top3.add(enabled);
            }
        }

        return top3;
    }

    private static void showAddKeywordPopup(Context context, AppViewModel viewModel, String keyword,
                                            List<Category> categories, java.util.function.Consumer<String> selectCategory) {
        Dialog dialog = new Dialog(context, R.style.Theme_ExpenseEye_Dialog);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_keyword, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvKeyword = dialogView.findViewById(R.id.tv_keyword_value);
        AutoCompleteTextView spinnerKeywordCat = dialogView.findViewById(R.id.spinner_keyword_category);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        tvKeyword.setText(keyword);

        List<String> catNamesList = new ArrayList<>();
        for (Category c : categories) {
            catNamesList.add(c.getName());
        }
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, catNamesList);
        spinnerKeywordCat.setAdapter(catAdapter);
        if (!catNamesList.isEmpty()) {
            spinnerKeywordCat.setText(catNamesList.get(0), false);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String selectedCatName = spinnerKeywordCat.getText().toString();
            int catId = -1;
            for (Category c : categories) {
                if (c.getName().equals(selectedCatName)) {
                    catId = c.getId();
                    break;
                }
            }

            if (catId != -1) {
                viewModel.insertCategoryKeyword(new CategoryKeyword(catId, keyword.toLowerCase().trim()));
                Toast.makeText(context, "Keyword added to " + selectedCatName, Toast.LENGTH_SHORT).show();
                selectCategory.accept(selectedCatName);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private static void setupPaymentMethodChipsForDialog(Context context, View dialogView, List<PaymentMethod> availableMethods, String selectedPaymentMethod) {
        RecyclerView rvMain = dialogView.findViewById(R.id.rv_payment_main);
        LinearLayout layoutOther = dialogView.findViewById(R.id.layout_payment_other);
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

        rvMain.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        rvMain.setAdapter(adapterMain);

        if ("Other".equals(initialMain)) {
            layoutOther.setVisibility(View.VISIBLE);
        }

        adapterOther[0] = new PaymentMethodAdapter(otherMethods, selectedPaymentMethod, name -> {});
        rvOther.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        rvOther.setAdapter(adapterOther[0]);
    }

    private static String getSelectedPaymentMethodFromDialog(View dialogView) {
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
