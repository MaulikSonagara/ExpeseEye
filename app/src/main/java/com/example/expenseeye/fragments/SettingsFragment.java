package com.example.expenseeye.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.adapters.ThemeSelectionAdapter;
import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.Budget;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.theme.ThemePreferenceHelper;
import com.example.expenseeye.utils.DatabaseBackupHelper;
import com.example.expenseeye.utils.ExportHelper;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.card.MaterialCardView;
import com.example.expenseeye.theme.ThemeManager;
import android.content.res.ColorStateList;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AutoCompleteTextView;
import androidx.core.util.Pair;
import java.util.Calendar;
import java.util.ArrayList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private ThemePreferenceHelper themeHelper;
    private AppViewModel viewModel;

    // Flag to determine which export requires permission upon callback
    private boolean pendingPdfExport = false;
    private boolean pendingCsvExport = false;

    // Activity result launcher for Storage Permission (Android 9 and below)
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    if (pendingPdfExport) {
                        pendingPdfExport = false;
                        exportPdfFlow();
                    } else if (pendingCsvExport) {
                        pendingCsvExport = false;
                        exportCsvFlow();
                    }
                } else {
                    pendingPdfExport = false;
                    pendingCsvExport = false;
                    Toast.makeText(getContext(), "Storage permission is required to save files on older devices.", Toast.LENGTH_LONG).show();
                }
            });

    // SAF backup file destination selector
    private final ActivityResultLauncher<Intent> backupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performBackup(uri);
                    }
                }
            });

    // SAF restore file picker
    private final ActivityResultLauncher<Intent> restoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performRestore(uri);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        themeHelper = new ThemePreferenceHelper(requireContext());
        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        setupThemeSelection(view);
        setupModeSwitch(view);
        setupEnhancedSettings(view);
        setupActionButtons(view);
    }

    private void setupEnhancedSettings(View view) {
        // Smart Classifier
        MaterialSwitch switchSmart = view.findViewById(R.id.switch_smart_classifier);
        if (switchSmart != null) {
            switchSmart.setChecked(themeHelper.isSmartClassifierEnabled());
            switchSmart.setOnCheckedChangeListener((buttonView, isChecked) -> {
                themeHelper.setSmartClassifierEnabled(isChecked);
            });
        }

        // Currency Symbol
        AutoCompleteTextView spinnerCurrency = view.findViewById(R.id.spinner_currency);
        if (spinnerCurrency != null) {
            String[] currencies = {"₹", "$", "€", "£", "¥", "₩", "₽"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies);
            spinnerCurrency.setAdapter(adapter);
            spinnerCurrency.setText(themeHelper.getCurrencySymbol(), false);
            spinnerCurrency.setOnClickListener(v -> spinnerCurrency.showDropDown());
            spinnerCurrency.setOnItemClickListener((parent, view1, position, id) -> {
                themeHelper.setCurrencySymbol(currencies[position]);
                // Toast to inform user it requires refresh for some parts
                Toast.makeText(getContext(), "Currency updated! Restart app to apply everywhere.", Toast.LENGTH_SHORT).show();
            });
        }

        // Default Payment Method
        AutoCompleteTextView spinnerPayment = view.findViewById(R.id.spinner_default_payment);
        if (spinnerPayment != null) {
            viewModel.getAllPaymentMethods().observe(getViewLifecycleOwner(), pms -> {
                if (pms == null || pms.isEmpty()) return;
                
                List<String> names = new ArrayList<>();
                names.add("None (Auto)");
                for (PaymentMethod pm : pms) {
                    names.add(pm.getName());
                }
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
                spinnerPayment.setAdapter(adapter);
                
                int savedId = themeHelper.getDefaultPaymentMethodId();
                String currentName = "None (Auto)";
                if (savedId != -1) {
                    for (PaymentMethod pm : pms) {
                        if (pm.getId() == savedId) {
                            currentName = pm.getName();
                            break;
                        }
                    }
                }
                spinnerPayment.setText(currentName, false);
                spinnerPayment.setOnClickListener(v -> spinnerPayment.showDropDown());
                
                spinnerPayment.setOnItemClickListener((parent, view1, position, id) -> {
                    if (position == 0) {
                        themeHelper.setDefaultPaymentMethodId(-1);
                    } else {
                        themeHelper.setDefaultPaymentMethodId(pms.get(position - 1).getId());
                    }
                });
            });
        }

        // Budgeting & Recurring Button Actions
        view.findViewById(R.id.btn_manage_budgets).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.example.expenseeye.BudgetsActivity.class);
            startActivity(intent);
        });
        view.findViewById(R.id.btn_manage_recurring).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.example.expenseeye.RecurringExpensesActivity.class);
            startActivity(intent);
        });

        // Set counts
        TextView tvBudgetCount = view.findViewById(R.id.tv_budget_count);
        TextView tvRecurringCount = view.findViewById(R.id.tv_recurring_count);

        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
        String currentMonth = sdf.format(new Date());

        viewModel.getBudgetCountLive(currentMonth).observe(getViewLifecycleOwner(), count -> {
            int c = count != null ? count : 0;
            tvBudgetCount.setText(c + " active");
        });

        viewModel.getRecurringExpenseCountLive().observe(getViewLifecycleOwner(), count -> {
            int c = count != null ? count : 0;
            tvRecurringCount.setText(c + " items");
        });
    }

    private void showManageBudgetsDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_manage_budgets, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setTitle("Monthly Budgets")
                .setPositiveButton("Close", null)
                .create();

        EditText etAmount = dialogView.findViewById(R.id.et_budget_amount);
        AutoCompleteTextView spinnerCategory = dialogView.findViewById(R.id.spinner_budget_category);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_budget);
        com.google.android.material.button.MaterialButton btnDelete = dialogView.findViewById(R.id.btn_delete_budget);

        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
        String currentMonth = sdf.format(new Date());

        // Setup categories
        viewModel.getEnabledCategories().observe(getViewLifecycleOwner(), cats -> {
            List<String> names = new ArrayList<>();
            names.add("Overall");
            for (Category c : cats) names.add(c.getName());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
            spinnerCategory.setAdapter(adapter);
            spinnerCategory.setText("Overall", false);
            
            // Initial load of overall budget
            loadBudgetForCategory("Overall", currentMonth, etAmount);
        });

        spinnerCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            loadBudgetForCategory(selected, currentMonth, etAmount);
        });

        btnSave.setOnClickListener(v -> {
            String amtStr = etAmount.getText().toString();
            if (amtStr.isEmpty()) return;
            
            double amount = Double.parseDouble(amtStr);
            String cat = spinnerCategory.getText().toString();
            
            new Thread(() -> {
                Budget existing = viewModel.getBudgetSync(currentMonth, cat);
                if (existing != null) {
                    existing.setAmount(amount);
                    viewModel.updateBudget(existing);
                } else {
                    viewModel.insertBudget(new Budget(amount, cat, currentMonth));
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Budget saved!", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        btnDelete.setOnClickListener(v -> {
            String cat = spinnerCategory.getText().toString();
            new Thread(() -> {
                Budget existing = viewModel.getBudgetSync(currentMonth, cat);
                if (existing != null) {
                    viewModel.deleteBudget(existing);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            etAmount.setText("");
                            Toast.makeText(getContext(), "Budget deleted", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }).start();
        });

        dialog.show();
    }

    private void loadBudgetForCategory(String category, String month, EditText etAmount) {
        new Thread(() -> {
            Budget b = viewModel.getBudgetSync(month, category);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (b != null) etAmount.setText(String.valueOf(b.getAmount()));
                    else etAmount.setText("");
                });
            }
        }).start();
    }

    private void setupThemeSelection(View view) {
        RecyclerView rvThemes = view.findViewById(R.id.rv_themes);
        android.widget.LinearLayout layoutThemeDots = view.findViewById(R.id.layout_theme_dots);
        List<String> themes = Arrays.asList(
                ThemePreferenceHelper.THEME_MIDNIGHT,
                ThemePreferenceHelper.THEME_FOREST,
                ThemePreferenceHelper.THEME_SAND,
                ThemePreferenceHelper.THEME_OCEAN
        );

        ThemeSelectionAdapter adapter = new ThemeSelectionAdapter(themes, themeHelper.getTheme(), themeName -> {
            themeHelper.setTheme(themeName);
            requireActivity().recreate();
        });

        rvThemes.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvThemes.setAdapter(adapter);
        setupRecyclerViewScrollIndicator(rvThemes, layoutThemeDots);

        // Disallow ViewPager2 from intercepting touch events when interacting with the themes recycler view
        rvThemes.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull android.view.MotionEvent e) {
                int action = e.getAction();
                if (action == android.view.MotionEvent.ACTION_DOWN) {
                    if (rv.getParent() != null) {
                        rv.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull android.view.MotionEvent e) {}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });
    }

    private void setupRecyclerViewScrollIndicator(RecyclerView recyclerView, android.widget.LinearLayout dotsLayout) {
        if (recyclerView == null || dotsLayout == null) return;
        
        // Initial state update
        recyclerView.post(() -> {
            int offset = recyclerView.computeHorizontalScrollOffset();
            int extent = recyclerView.computeHorizontalScrollExtent();
            int range = recyclerView.computeHorizontalScrollRange();
            int maxScroll = range - extent;
            float progress = maxScroll > 0 ? (float) offset / maxScroll : 0f;
            updateScrollDots(dotsLayout, progress);
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                int offset = rv.computeHorizontalScrollOffset();
                int extent = rv.computeHorizontalScrollExtent();
                int range = rv.computeHorizontalScrollRange();
                int maxScroll = range - extent;
                if (maxScroll > 0) {
                    float progress = (float) offset / maxScroll;
                    updateScrollDots(dotsLayout, progress);
                } else {
                    updateScrollDots(dotsLayout, 0f);
                }
            }
        });
    }

    private void updateScrollDots(android.widget.LinearLayout dotsLayout, float progress) {
        int childCount = dotsLayout.getChildCount();
        if (childCount == 0) return;
        float activeIndex = progress * (childCount - 1);
        for (int i = 0; i < childCount; i++) {
            View dot = dotsLayout.getChildAt(i);
            float distance = Math.abs(i - activeIndex);
            // active dot has scale 1.15, others range down to 0.75
            float scale = 1.15f - Math.min(distance * 0.25f, 0.40f);
            dot.setScaleX(scale);
            dot.setScaleY(scale);
            
            // set active dot to full opacity, inactive to low opacity
            float alpha = 1.0f - Math.min(distance * 0.4f, 0.6f);
            dot.setAlpha(alpha);
        }
    }

    private void setupModeSwitch(View view) {
        MaterialSwitch switchDark = view.findViewById(R.id.switch_dark_mode);
        switchDark.setChecked(themeHelper.isDarkMode());

        switchDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeHelper.setMode(isChecked ? ThemePreferenceHelper.MODE_DARK : ThemePreferenceHelper.MODE_LIGHT);
            requireActivity().recreate();
        });

        MaterialSwitch switchSuggestions = view.findViewById(R.id.switch_title_suggestions);
        switchSuggestions.setChecked(themeHelper.isTitleSuggestionsEnabled());
        switchSuggestions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeHelper.setTitleSuggestionsEnabled(isChecked);
        });
    }

    private void setupActionButtons(View view) {
        view.findViewById(R.id.btn_export_pdf).setOnClickListener(v -> exportPdfFlow());
        view.findViewById(R.id.btn_export_csv).setOnClickListener(v -> exportCsvFlow());
        
        view.findViewById(R.id.btn_backup).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/x-sqlite3");
            String fileName = "expense_eye_backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".db";
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            backupLauncher.launch(intent);
        });

        view.findViewById(R.id.btn_restore).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            restoreLauncher.launch(intent);
        });

        view.findViewById(R.id.btn_manage_categories).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.example.expenseeye.CategoryManagementActivity.class);
            startActivity(intent);
        });
    }

    // --- PDF Export Flow ---
    private void exportPdfFlow() {
        if (!checkStoragePermission()) {
            pendingPdfExport = true;
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pdf_date_range, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Initialize UI Elements
        AutoCompleteTextView actvMonth = dialogView.findViewById(R.id.actv_month);
        AutoCompleteTextView actvYear = dialogView.findViewById(R.id.actv_year);

        TextInputLayout tilFromDate = dialogView.findViewById(R.id.til_from_date);
        TextInputEditText etFromDate = dialogView.findViewById(R.id.et_from_date);
        TextInputLayout tilToDate = dialogView.findViewById(R.id.til_to_date);
        TextInputEditText etToDate = dialogView.findViewById(R.id.et_to_date);

        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnExport = dialogView.findViewById(R.id.btn_export);

        // Prepopulate month & year options
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        
        Calendar currentCal = Calendar.getInstance();
        int currentYear = currentCal.get(Calendar.YEAR);
        ArrayList<String> yearsList = new ArrayList<>();
        for (int y = 2023; y <= currentYear; y++) {
            yearsList.add(String.valueOf(y));
        }
        String[] years = yearsList.toArray(new String[0]);

        android.widget.ArrayAdapter<String> monthAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, months);
        android.widget.ArrayAdapter<String> yearAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, years);

        actvMonth.setAdapter(monthAdapter);
        actvYear.setAdapter(yearAdapter);

        // Set Default Date Range for custom fields
        SimpleDateFormat dateStrFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        Calendar dateRangeCal = Calendar.getInstance();
        String todayStr = dateStrFormat.format(dateRangeCal.getTime());
        dateRangeCal.set(Calendar.DAY_OF_MONTH, 1);
        String firstDayStr = dateStrFormat.format(dateRangeCal.getTime());
        etFromDate.setText(firstDayStr);
        etToDate.setText(todayStr);

        // Set current month/year selection by default in specific month dropdowns
        actvMonth.setText(months[currentCal.get(Calendar.MONTH)], false);
        actvYear.setText(String.valueOf(currentCal.get(Calendar.YEAR)), false);

        // Selection state holder
        final String[] selectedOption = {"This Month"};

        // Initial visual style setup
        selectOption(selectedOption[0], dialogView, requireContext());

        // Card option click listeners
        dialogView.findViewById(R.id.opt_today).setOnClickListener(v -> {
            selectedOption[0] = "Today";
            selectOption(selectedOption[0], dialogView, requireContext());
        });
        dialogView.findViewById(R.id.opt_last_30).setOnClickListener(v -> {
            selectedOption[0] = "Last 30 Days";
            selectOption(selectedOption[0], dialogView, requireContext());
        });
        dialogView.findViewById(R.id.opt_this_month).setOnClickListener(v -> {
            selectedOption[0] = "This Month";
            selectOption(selectedOption[0], dialogView, requireContext());
        });
        dialogView.findViewById(R.id.opt_spec_month).setOnClickListener(v -> {
            selectedOption[0] = "Specific Month";
            selectOption(selectedOption[0], dialogView, requireContext());
        });
        dialogView.findViewById(R.id.opt_range).setOnClickListener(v -> {
            selectedOption[0] = "Custom Range";
            selectOption(selectedOption[0], dialogView, requireContext());
        });
        dialogView.findViewById(R.id.opt_all_data).setOnClickListener(v -> {
            selectedOption[0] = "All Data";
            selectOption(selectedOption[0], dialogView, requireContext());
        });

        // Custom Date Range Picker triggers
        tilFromDate.setEndIconOnClickListener(v -> showDatePicker(etFromDate));
        etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        tilToDate.setEndIconOnClickListener(v -> showDatePicker(etToDate));
        etToDate.setOnClickListener(v -> showDatePicker(etToDate));

        // Action Buttons
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnExport.setOnClickListener(v -> {
            long startDate = 0;
            long endDate = 0;
            Calendar cal = Calendar.getInstance();

            switch (selectedOption[0]) {
                case "Today":
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    startDate = cal.getTimeInMillis();

                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    endDate = cal.getTimeInMillis();
                    break;

                case "Last 30 Days":
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    endDate = cal.getTimeInMillis();

                    cal.add(Calendar.DAY_OF_YEAR, -30);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    startDate = cal.getTimeInMillis();
                    break;

                case "This Month":
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    endDate = cal.getTimeInMillis();

                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    startDate = cal.getTimeInMillis();
                    break;

                case "All Data":
                    startDate = 0;
                    endDate = System.currentTimeMillis();
                    break;

                case "Specific Month":
                    String monthStr = actvMonth.getText().toString();
                    String yearStr = actvYear.getText().toString();
                    int monthIdx = getSelectedMonthIndex(monthStr);
                    if (monthIdx < 0 || yearStr.isEmpty()) {
                        showToastOnMainThread("Please select both month and year.");
                        return;
                    }
                    try {
                        int year = Integer.parseInt(yearStr);
                        cal.clear();
                        cal.set(Calendar.YEAR, year);
                        cal.set(Calendar.MONTH, monthIdx);
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        startDate = cal.getTimeInMillis();

                        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                        cal.set(Calendar.DAY_OF_MONTH, maxDay);
                        cal.set(Calendar.HOUR_OF_DAY, 23);
                        cal.set(Calendar.MINUTE, 59);
                        cal.set(Calendar.SECOND, 59);
                        cal.set(Calendar.MILLISECOND, 999);
                        endDate = cal.getTimeInMillis();

                        long now = System.currentTimeMillis();
                        if (startDate > now) {
                            showToastOnMainThread("Future periods cannot be exported.");
                            return;
                        }
                        if (endDate > now) {
                            endDate = now;
                        }
                    } catch (Exception e) {
                        showToastOnMainThread("Error parsing month/year.");
                        return;
                    }
                    break;

                case "Custom Range":
                    String fromStr = etFromDate.getText().toString().trim();
                    String toStr = etToDate.getText().toString().trim();

                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    sdf.setLenient(false);

                    try {
                        Date fromDate = sdf.parse(fromStr);
                        Date toDate = sdf.parse(toStr);

                        if (fromDate == null || toDate == null) {
                            showToastOnMainThread("Invalid date format. Use DD-MM-YYYY");
                            return;
                        }

                        if (fromDate.after(toDate)) {
                            showToastOnMainThread("From Date cannot be after To Date.");
                            return;
                        }

                        startDate = fromDate.getTime();

                        Calendar endCal = Calendar.getInstance();
                        endCal.setTime(toDate);
                        endCal.set(Calendar.HOUR_OF_DAY, 23);
                        endCal.set(Calendar.MINUTE, 59);
                        endCal.set(Calendar.SECOND, 59);
                        endCal.set(Calendar.MILLISECOND, 999);
                        endDate = endCal.getTimeInMillis();

                        long now = System.currentTimeMillis();
                        if (startDate > now) {
                            showToastOnMainThread("Future dates cannot be exported.");
                            return;
                        }
                        if (endDate > now) {
                            endDate = now;
                        }

                    } catch (Exception e) {
                        showToastOnMainThread("Please enter valid dates in DD-MM-YYYY format.");
                        return;
                    }
                    break;
            }

            dialog.dismiss();
            generatePdfForRange(startDate, endDate);
        });

        dialog.show();
    }

    private void selectOption(String option, View dialogView, Context ctx) {
        String[] options = {"Today", "Last 30 Days", "This Month", "Specific Month", "Custom Range", "All Data"};
        int[] cardIds = {R.id.opt_today, R.id.opt_last_30, R.id.opt_this_month, R.id.opt_spec_month, R.id.opt_range, R.id.opt_all_data};
        int[] tvIds = {R.id.tv_opt_today, R.id.tv_opt_last_30, R.id.tv_opt_this_month, R.id.tv_opt_spec_month, R.id.tv_opt_range, R.id.tv_opt_all_data};
        int[] ivIds = {R.id.iv_opt_today, R.id.iv_opt_last_30, R.id.iv_opt_this_month, R.id.iv_opt_spec_month, R.id.iv_opt_range, R.id.iv_opt_all_data};

        int primaryColor = ThemeManager.getColor(ctx, ThemeManager.ThemeColor.PRIMARY);
        int textSecondary = ThemeManager.getColor(ctx, ThemeManager.ThemeColor.TEXT_SECONDARY);
        int dividerColor = ThemeManager.getColor(ctx, ThemeManager.ThemeColor.DIVIDER);
        int surfaceColor = ThemeManager.getColor(ctx, ThemeManager.ThemeColor.SURFACE);
        int elevatedSurface = ThemeManager.getColor(ctx, ThemeManager.ThemeColor.ELEVATED_SURFACE);

        float density = ctx.getResources().getDisplayMetrics().density;
        int strokeWidthSelected = Math.round(3 * density);
        int strokeWidthUnselected = Math.round(1.5f * density);

        for (int i = 0; i < options.length; i++) {
            MaterialCardView card = dialogView.findViewById(cardIds[i]);
            TextView tv = dialogView.findViewById(tvIds[i]);
            ImageView iv = dialogView.findViewById(ivIds[i]);

            boolean isSelected = options[i].equals(option);

            if (isSelected) {
                card.setCardBackgroundColor(ColorStateList.valueOf(elevatedSurface));
                card.setStrokeColor(ColorStateList.valueOf(primaryColor));
                card.setStrokeWidth(strokeWidthSelected);
                tv.setTextColor(primaryColor);
                iv.setImageTintList(ColorStateList.valueOf(primaryColor));
                card.setCardElevation(2 * density);
            } else {
                card.setCardBackgroundColor(ColorStateList.valueOf(surfaceColor));
                card.setStrokeColor(ColorStateList.valueOf(dividerColor));
                card.setStrokeWidth(strokeWidthUnselected);
                tv.setTextColor(textSecondary);
                iv.setImageTintList(ColorStateList.valueOf(textSecondary));
                card.setCardElevation(0);
            }
        }

        View specMonthPanel = dialogView.findViewById(R.id.section_specific_month);
        View customRangePanel = dialogView.findViewById(R.id.section_custom_range);

        if ("Specific Month".equals(option)) {
            specMonthPanel.setVisibility(View.VISIBLE);
            customRangePanel.setVisibility(View.GONE);
        } else if ("Custom Range".equals(option)) {
            specMonthPanel.setVisibility(View.GONE);
            customRangePanel.setVisibility(View.VISIBLE);
        } else {
            specMonthPanel.setVisibility(View.GONE);
            customRangePanel.setVisibility(View.GONE);
        }
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar cal = Calendar.getInstance();
        String txt = editText.getText().toString().trim();
        if (!txt.isEmpty()) {
            try {
                String[] parts = txt.split("-");
                if (parts.length == 3) {
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]) - 1;
                    int year = Integer.parseInt(parts[2]);
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                }
            } catch (Exception ignored) {}
        }

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String formattedDate = String.format(Locale.getDefault(), "%02d-%02d-%04d", dayOfMonth, month + 1, year);
            editText.setText(formattedDate);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateSpecificMonthRange(int monthIndex, String yearStr, TextInputEditText etFrom, TextInputEditText etTo) {
        if (monthIndex < 0 || yearStr.isEmpty()) return;
        try {
            int year = Integer.parseInt(yearStr);
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, monthIndex);
            cal.set(Calendar.DAY_OF_MONTH, 1);

            SimpleDateFormat dateStrFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            String firstDay = dateStrFormat.format(cal.getTime());

            int lastDayVal = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            cal.set(Calendar.DAY_OF_MONTH, lastDayVal);
            String lastDay = dateStrFormat.format(cal.getTime());

            etFrom.setText(firstDay);
            etTo.setText(lastDay);
        } catch (Exception ignored) {}
    }

    private int getSelectedMonthIndex(String monthName) {
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        for (int i = 0; i < months.length; i++) {
            if (months[i].equals(monthName)) return i;
        }
        return -1;
    }

    private void generatePdfForRange(long startDate, long endDate) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Expense> allExpenses = viewModel.getAllExpensesSync();
            if (allExpenses == null || allExpenses.isEmpty()) {
                showToastOnMainThread("No expenses to export.");
                return;
            }

            // Filter in-range expenses
            List<Expense> expenses = new ArrayList<>();
            for (Expense e : allExpenses) {
                if (e.getTimestamp() >= startDate && e.getTimestamp() <= endDate) {
                    expenses.add(e);
                }
            }

            if (expenses.isEmpty()) {
                showToastOnMainThread("No expenses found in the selected range.");
                return;
            }

            try {
                File tempFile = new File(requireContext().getCacheDir(), "expense_report_" + System.currentTimeMillis() + ".pdf");
                boolean exportSuccess = ExportHelper.exportToPDF(expenses, tempFile, startDate, endDate);
                if (exportSuccess) {
                    String fileName = "ExpenseEye_Report_" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()) + ".pdf";
                    boolean saveSuccess = saveFileToDownloads(tempFile, fileName, "application/pdf");
                    if (saveSuccess) {
                        showToastOnMainThread("PDF Saved successfully in Downloads.");
                        shareFile(tempFile, "application/pdf", "Share PDF Report");
                    } else {
                        showToastOnMainThread("Failed to save PDF to storage.");
                    }
                } else {
                    showToastOnMainThread("Failed to generate PDF Report.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToastOnMainThread("Error occurred during PDF generation.");
            }
        });
    }

    // --- CSV Export Flow ---
    private void exportCsvFlow() {
        if (!checkStoragePermission()) {
            pendingCsvExport = true;
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Expense> expenses = viewModel.getAllExpensesSync();
            if (expenses == null || expenses.isEmpty()) {
                showToastOnMainThread("No expenses to export.");
                return;
            }

            try {
                File tempFile = new File(requireContext().getCacheDir(), "expense_sheet_" + System.currentTimeMillis() + ".csv");
                boolean exportSuccess = ExportHelper.exportToCSV(expenses, tempFile);
                if (exportSuccess) {
                    String fileName = "ExpenseEye_Sheet_" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()) + ".csv";
                    boolean saveSuccess = saveFileToDownloads(tempFile, fileName, "text/csv");
                    if (saveSuccess) {
                        showToastOnMainThread("CSV Saved successfully in Downloads.");
                        shareFile(tempFile, "text/csv", "Share CSV Sheet");
                    } else {
                        showToastOnMainThread("Failed to save CSV to storage.");
                    }
                } else {
                    showToastOnMainThread("Failed to generate CSV Sheet.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToastOnMainThread("Error occurred during CSV generation.");
            }
        });
    }

    // --- Database Backup ---
    private void performBackup(Uri targetUri) {
        new Thread(() -> {
            boolean success = DatabaseBackupHelper.backupDatabase(requireContext(), targetUri);
            if (success) {
                showToastOnMainThread("Database backup created successfully!");
            } else {
                showToastOnMainThread("Database backup failed.");
            }
        }).start();
    }

    // --- Database Restoration ---
    private void performRestore(Uri sourceUri) {
        new Thread(() -> {
            // 1. Validate file before restoring
            DatabaseBackupHelper.ValidationResult result = DatabaseBackupHelper.validateBackupFile(requireContext(), sourceUri);
            if (!result.isValid) {
                showToastOnMainThread("Restore failed: " + result.errorMessage);
                return;
            }

            // 2. Perform safe restore
            boolean success = DatabaseBackupHelper.restoreDatabase(requireContext(), sourceUri);
            if (success) {
                showToastOnMainThread("Database restored (v" + result.version + ") successfully! Restarting app...");
                
                // Restart app on main thread to apply changes cleanly
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(getActivity().getPackageName());
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                        getActivity().finishAffinity();
                        System.exit(0);
                    });
                }
            } else {
                showToastOnMainThread("Database restore failed.");
            }
        }).start();
    }

    // --- Helpers ---
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            // Android 10+ (Q) uses Scoped Storage, no storage permissions needed for Downloads directory
            return true;
        }
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean saveFileToDownloads(File sourceFile, String displayName, String mimeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                    if (out == null) return false;
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            File targetFile = new File(downloadsDir, displayName);
            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void shareFile(File file, String mimeType, String chooserTitle) {
        try {
            Uri fileUri = FileProvider.getUriForFile(requireContext(), "com.example.expenseeye.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, chooserTitle));
        } catch (Exception e) {
            e.printStackTrace();
            showToastOnMainThread("Failed to share file.");
        }
    }

    private void showToastOnMainThread(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }
}
