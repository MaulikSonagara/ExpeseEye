package com.example.expenseeye.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.adapters.ExpenseAdapter;
import com.example.expenseeye.models.Budget;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.theme.ThemePreferenceHelper;
import com.example.expenseeye.utils.ExpenseClassifier;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.example.expenseeye.adapters.PaymentMethodAdapter;
import com.example.expenseeye.utils.KeyboardFollow;

import com.example.expenseeye.utils.ExpenseDialogHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private AppViewModel viewModel;
    private ExpenseAdapter adapter;
    private TextView tvMonthTotal, tvTodayTotal, tvWeekTotal, tvYearTotal, tvComparison, tvNoExpenses;
    private TextView tvMonthBudgetLimit, tvEstSavings;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressMonthlyBudget;
    private android.widget.LinearLayout layoutBudgetContainer;
    private View tvBudgetHeader, cardBudgetContainer;
    private RecyclerView rvRecentExpenses;
    private TextView tvGreeting, tvGreetingSub, tvActiveTripName;
    private View cardActiveTrip;
    private android.widget.LinearLayout layoutDebtContainer;
    private double totalOwedToOthers = 0.0;
    private double totalOwedToMe = 0.0;
    private final android.os.Handler greetingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable greetingRunnable;
    private List<Category> availableCategories = new ArrayList<>();
    private List<PaymentMethod> availablePaymentMethods = new ArrayList<>();
    private List<com.example.expenseeye.models.CategoryKeyword> allKeywords = new ArrayList<>();
    private List<Expense> currentExpenses = new ArrayList<>();
    private List<Budget> currentBudgets = new ArrayList<>();

    // Variables for picker in BottomSheet dialog
    private Calendar selectedDateTime = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Bind views
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvGreetingSub = view.findViewById(R.id.tv_greeting_sub);
        cardActiveTrip = view.findViewById(R.id.card_active_trip);
        tvActiveTripName = view.findViewById(R.id.tv_active_trip_name);
        layoutDebtContainer = view.findViewById(R.id.layout_debt_container);
        tvMonthTotal = view.findViewById(R.id.tv_month_total);
        tvTodayTotal = view.findViewById(R.id.tv_today_total);
        tvWeekTotal = view.findViewById(R.id.tv_week_total);
        tvYearTotal = view.findViewById(R.id.tv_year_total);
        tvComparison = view.findViewById(R.id.tv_comparison);
        tvNoExpenses = view.findViewById(R.id.tv_no_expenses);
        rvRecentExpenses = view.findViewById(R.id.rv_recent_expenses);
        layoutBudgetContainer = view.findViewById(R.id.layout_budget_container);
        tvBudgetHeader = view.findViewById(R.id.tv_budget_header);
        cardBudgetContainer = view.findViewById(R.id.card_budget_container);
        FloatingActionButton fabAddExpense = view.findViewById(R.id.fab_add_expense);
        TextView btnSeeAll = view.findViewById(R.id.btn_see_all);

        // New monthly summary card elements
        tvMonthBudgetLimit = view.findViewById(R.id.tv_month_budget_limit);
        tvEstSavings = view.findViewById(R.id.tv_est_savings);
        progressMonthlyBudget = view.findViewById(R.id.progress_monthly_budget);

        final TextView tvDashOwe = view.findViewById(R.id.tv_dash_owe);
        final TextView tvDashOwed = view.findViewById(R.id.tv_dash_owed);
        View cardOwe = view.findViewById(R.id.card_owe);
        View cardOwed = view.findViewById(R.id.card_owed);
        com.example.expenseeye.theme.ThemePreferenceHelper prefHelper = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
        final String currencySymbol = prefHelper.getCurrencySymbol();

        View.OnClickListener borrowOweClick = v -> {
            Intent intent = new Intent(getActivity(), com.example.expenseeye.BorrowOweActivity.class);
            startActivity(intent);
        };
        if (cardOwe != null) cardOwe.setOnClickListener(borrowOweClick);
        if (cardOwed != null) cardOwed.setOnClickListener(borrowOweClick);

        View btnViewDetails = view.findViewById(R.id.btn_view_details);
        if (btnViewDetails != null) {
            btnViewDetails.setOnClickListener(v -> {
                if (getActivity() != null) {
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                            getActivity().findViewById(R.id.bottom_navigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.expensesFragment);
                    }
                }
            });
        }

        View btnEditBudgets = view.findViewById(R.id.btn_edit_budgets);
        if (btnEditBudgets != null) {
            btnEditBudgets.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), com.example.expenseeye.BudgetsActivity.class);
                startActivity(intent);
            });
        }

        // Set up recycler view
        rvRecentExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExpenseAdapter(this::showEditExpenseBottomSheet);
        rvRecentExpenses.setAdapter(adapter);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        // Observe Borrow/Owe stats
        viewModel.getTotalOwedToOthers().observe(getViewLifecycleOwner(), owe -> {
            double val = owe != null ? owe : 0.0;
            totalOwedToOthers = val;
            if (tvDashOwe != null) {
                tvDashOwe.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, val));
            }
            updateDebtContainerVisibility();
        });

        viewModel.getTotalOwedToMe().observe(getViewLifecycleOwner(), owed -> {
            double val = owed != null ? owed : 0.0;
            totalOwedToMe = val;
            if (tvDashOwed != null) {
                tvDashOwed.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, val));
            }
            updateDebtContainerVisibility();
        });

        // Observe Categories & PaymentMethods for spinners cache
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                availableCategories = categories;
                adapter.setCategories(categories);
            }
        });

        // Store enabled categories and keywords for add/edit dialogs
        final List<Category>[] enabledCategories = new List[]{new ArrayList<>()};
        viewModel.getEnabledCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                enabledCategories[0] = categories;
            }
        });

        viewModel.getAllKeywords().observe(getViewLifecycleOwner(), keywords -> {
            if (keywords != null) {
                allKeywords = keywords;
            }
        });

        viewModel.getAllPaymentMethods().observe(getViewLifecycleOwner(), pms -> {
            if (pms != null) {
                availablePaymentMethods = pms;
            }
        });

        // Observe all expenses to calculate totals, list recent, and update budget
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                currentExpenses = expenses;
                if (!expenses.isEmpty()) {
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
                refreshBudgetSection();
            }
        });

        // Observe Budgets for the current month independently
        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
        String currentMonth = sdf.format(new Date());
        viewModel.getBudgetsForMonth(currentMonth).observe(getViewLifecycleOwner(), budgets -> {
            if (budgets != null) {
                currentBudgets = budgets;
                refreshBudgetSection();
            }
        });

        // See All action
        btnSeeAll.setOnClickListener(v -> {
            if (getActivity() != null) {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                        getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.expensesFragment);
                }
            }
        });

        // FAB Quick Add action
        fabAddExpense.setOnClickListener(v -> showAddExpenseBottomSheet());

        // Hamburger Menu action
        ImageButton btnMenu = view.findViewById(R.id.btn_hamburger_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(this::showHamburgerMenu);
        }

        viewModel.getActiveTrip().observe(getViewLifecycleOwner(), trip -> {
            if (trip != null) {
                cardActiveTrip.setVisibility(View.VISIBLE);
                tvActiveTripName.setText(trip.getTitle());
                view.findViewById(R.id.btn_active_trip_details).setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), com.example.expenseeye.TripDetailsActivity.class);
                    intent.putExtra("TRIP_ID", trip.getId());
                    startActivity(intent);
                });
            } else {
                cardActiveTrip.setVisibility(View.GONE);
            }
        });

        return view;
    }

    private void showHamburgerMenu(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        popup.getMenu().add(0, 1, 0, "Monthly Budgets");
        popup.getMenu().add(0, 2, 1, "Reminder Expenses");
        popup.getMenu().add(0, 3, 2, "Borrow & Owe");
        popup.getMenu().add(0, 4, 3, "Trips");
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                Intent intent = new Intent(getActivity(), com.example.expenseeye.BudgetsActivity.class);
                startActivity(intent);
                return true;
            } else if (item.getItemId() == 2) {
                Intent intent = new Intent(getActivity(), com.example.expenseeye.ReminderExpensesActivity.class);
                startActivity(intent);
                return true;
            } else if (item.getItemId() == 3) {
                Intent intent = new Intent(getActivity(), com.example.expenseeye.BorrowOweActivity.class);
                startActivity(intent);
                return true;
            } else if (item.getItemId() == 4) {
                Intent intent = new Intent(getActivity(), com.example.expenseeye.TripsActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void calculateTotals(List<Expense> expenses) {
        double todayTotal = 0;
        double weekTotal = 0;
        double monthTotal = 0;
        double yearTotal = 0;

        long now = System.currentTimeMillis();

        Calendar calNow = Calendar.getInstance();
        int currentDay = calNow.get(Calendar.DAY_OF_YEAR);
        int currentWeek = calNow.get(Calendar.WEEK_OF_YEAR);
        int currentMonth = calNow.get(Calendar.MONTH);
        int currentYear = calNow.get(Calendar.YEAR);

        Calendar calExp = Calendar.getInstance();

        for (Expense e : expenses) {
            calExp.setTimeInMillis(e.getTimestamp());
            int expYear = calExp.get(Calendar.YEAR);

            if (expYear == currentYear) {
                yearTotal += e.getAmount();

                if (calExp.get(Calendar.MONTH) == currentMonth) {
                    monthTotal += e.getAmount();
                }

                if (calExp.get(Calendar.WEEK_OF_YEAR) == currentWeek) {
                    weekTotal += e.getAmount();
                }

                if (calExp.get(Calendar.DAY_OF_YEAR) == currentDay) {
                    todayTotal += e.getAmount();
                }
            }
        }

        com.example.expenseeye.theme.ThemePreferenceHelper prefHelper = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
        String currencySymbol = prefHelper.getCurrencySymbol();

        if (tvTodayTotal != null) tvTodayTotal.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, todayTotal));
        if (tvWeekTotal != null) tvWeekTotal.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, weekTotal));
        if (tvMonthTotal != null) tvMonthTotal.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, monthTotal));
        if (tvYearTotal != null) tvYearTotal.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, yearTotal));

        // Comparison comparison logic
        double lastMonthTotal = 0;
        Calendar calLastMonth = Calendar.getInstance();
        calLastMonth.add(Calendar.MONTH, -1);
        int lmMonth = calLastMonth.get(Calendar.MONTH);
        int lmYear = calLastMonth.get(Calendar.YEAR);

        for (Expense e : expenses) {
            calExp.setTimeInMillis(e.getTimestamp());
            if (calExp.get(Calendar.YEAR) == lmYear && calExp.get(Calendar.MONTH) == lmMonth) {
                lastMonthTotal += e.getAmount();
            }
        }

        if (tvComparison != null) {
            if (lastMonthTotal > 0) {
                double percent = ((monthTotal - lastMonthTotal) / lastMonthTotal) * 100;
                if (percent > 0) {
                    tvComparison.setText(String.format(Locale.getDefault(), "▲ %.1f%% more than last month", percent));
                    tvComparison.setTextColor(com.example.expenseeye.theme.ThemeManager.getColor(getContext(), com.example.expenseeye.theme.ThemeManager.ThemeColor.DANGER));
                } else if (percent < 0) {
                    tvComparison.setText(String.format(Locale.getDefault(), "▼ %.1f%% less than last month", Math.abs(percent)));
                    tvComparison.setTextColor(com.example.expenseeye.theme.ThemeManager.getColor(getContext(), com.example.expenseeye.theme.ThemeManager.ThemeColor.SUCCESS));
                } else {
                    tvComparison.setText("Same as last month");
                    tvComparison.setTextColor(com.example.expenseeye.theme.ThemeManager.getColor(getContext(), com.example.expenseeye.theme.ThemeManager.ThemeColor.TEXT_SECONDARY));
                }
            } else {
                tvComparison.setText("No previous month data");
                tvComparison.setTextColor(com.example.expenseeye.theme.ThemeManager.getColor(getContext(), com.example.expenseeye.theme.ThemeManager.ThemeColor.TEXT_SECONDARY));
            }
        }
    }

    private void resetDashboardTotals() {
        com.example.expenseeye.theme.ThemePreferenceHelper prefHelper = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
        String currencySymbol = prefHelper.getCurrencySymbol();
        if (tvTodayTotal != null) tvTodayTotal.setText(currencySymbol + "0.00");
        if (tvWeekTotal != null) tvWeekTotal.setText(currencySymbol + "0.00");
        if (tvMonthTotal != null) tvMonthTotal.setText(currencySymbol + "0.00");
        if (tvYearTotal != null) tvYearTotal.setText(currencySymbol + "0.00");
        if (tvComparison != null) tvComparison.setText("No data");
    }

    private void refreshBudgetSection() {
        if (layoutBudgetContainer == null) return;
        layoutBudgetContainer.removeAllViews();

        // Calculate total category spending for the current month
        Map<String, Double> spendingMap = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);
        double overallSpent = 0.0;

        for (Expense e : currentExpenses) {
            cal.setTimeInMillis(e.getTimestamp());
            if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                String catName = e.getCategoryName();
                spendingMap.put(catName, spendingMap.getOrDefault(catName, 0.0) + e.getAmount());
                overallSpent += e.getAmount();
            }
        }

        com.example.expenseeye.theme.ThemePreferenceHelper prefHelper = new com.example.expenseeye.theme.ThemePreferenceHelper(requireContext());
        String currency = prefHelper.getCurrencySymbol();

        // Find overall budget limit and category budgets
        double overallBudgetLimit = 0.0;
        List<Budget> categoryBudgets = new ArrayList<>();
        if (currentBudgets != null) {
            for (Budget budget : currentBudgets) {
                if ("Overall".equalsIgnoreCase(budget.getCategoryName())) {
                    overallBudgetLimit = budget.getAmount();
                } else {
                    categoryBudgets.add(budget);
                }
            }
        }

        // Update Monthly Summary Card
        if (tvMonthBudgetLimit != null) {
            if (overallBudgetLimit > 0) {
                tvMonthBudgetLimit.setText(String.format(Locale.getDefault(), " / %s%.0f", currency, overallBudgetLimit));
            } else {
                tvMonthBudgetLimit.setText(" / Not Set");
            }
        }

        if (progressMonthlyBudget != null) {
            if (overallBudgetLimit > 0) {
                int progress = (int) ((overallSpent / overallBudgetLimit) * 100);
                progressMonthlyBudget.setProgress(Math.min(progress, 100));
                if (overallSpent > overallBudgetLimit) {
                    progressMonthlyBudget.setIndicatorColor(com.example.expenseeye.theme.ThemeManager.getColor(getContext(), com.example.expenseeye.theme.ThemeManager.ThemeColor.DANGER));
                } else if (progress > 80) {
                    progressMonthlyBudget.setIndicatorColor(com.example.expenseeye.theme.ThemeManager.getColor(getContext(), com.example.expenseeye.theme.ThemeManager.ThemeColor.WARNING));
                } else {
                    progressMonthlyBudget.setIndicatorColor(com.example.expenseeye.theme.ThemeManager.getColor(getContext(), com.example.expenseeye.theme.ThemeManager.ThemeColor.PRIMARY));
                }
            } else {
                progressMonthlyBudget.setProgress(0);
            }
        }

        if (tvEstSavings != null) {
            if (overallBudgetLimit > 0) {
                double savings = overallBudgetLimit - overallSpent;
                tvEstSavings.setText(String.format(Locale.getDefault(), "%s%.2f", currency, Math.max(0.0, savings)));
            } else {
                tvEstSavings.setText("--");
            }
        }

        // Now populate category-specific budgets under Budget Progress Card
        if (categoryBudgets.isEmpty()) {
            if (tvBudgetHeader != null) tvBudgetHeader.setVisibility(View.GONE);
            if (cardBudgetContainer != null) cardBudgetContainer.setVisibility(View.GONE);
            return;
        }

        if (tvBudgetHeader != null) tvBudgetHeader.setVisibility(View.VISIBLE);
        if (cardBudgetContainer != null) cardBudgetContainer.setVisibility(View.VISIBLE);

        for (Budget budget : categoryBudgets) {
            View budgetView = getLayoutInflater().inflate(R.layout.item_dashboard_budget, layoutBudgetContainer, false);
            TextView tvCat = budgetView.findViewById(R.id.tv_budget_label);
            TextView tvRemaining = budgetView.findViewById(R.id.tv_budget_remaining);
            com.google.android.material.progressindicator.LinearProgressIndicator progressIndicator = budgetView.findViewById(R.id.progress_budget);
            ImageView ivIcon = budgetView.findViewById(R.id.iv_budget_icon);

            double spent = spendingMap.getOrDefault(budget.getCategoryName(), 0.0);
            tvCat.setText(budget.getCategoryName());
            tvRemaining.setText(String.format(Locale.getDefault(), "%s%.2f / %s%.2f", currency, spent, currency, budget.getAmount()));

            int progress = (int) ((spent / budget.getAmount()) * 100);
            progressIndicator.setProgress(Math.min(progress, 100));

            // Set dynamic icon on left
            Category cat = null;
            if (availableCategories != null) {
                for (Category c : availableCategories) {
                    if (c.getName().equalsIgnoreCase(budget.getCategoryName())) {
                        cat = c;
                        break;
                    }
                }
            }
            int color = Color.parseColor("#9E9E9E"); // Default grey
            String iconName = "ic_other";
            if (cat != null) {
                color = cat.getColor();
                iconName = cat.getIconName();
            }
            if (ivIcon != null) {
                int resId = getContext().getResources().getIdentifier(iconName, "drawable", getContext().getPackageName());
                if (resId != 0) {
                    ivIcon.setImageResource(resId);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_other);
                }
                ivIcon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            }

            if (spent > budget.getAmount()) {
                progressIndicator.setIndicatorColor(com.example.expenseeye.theme.ThemeManager.getColor(getContext(), com.example.expenseeye.theme.ThemeManager.ThemeColor.DANGER));
            } else if (progress > 80) {
                progressIndicator.setIndicatorColor(com.example.expenseeye.theme.ThemeManager.getColor(getContext(), com.example.expenseeye.theme.ThemeManager.ThemeColor.WARNING));
            } else {
                progressIndicator.setIndicatorColor(com.example.expenseeye.theme.ThemeManager.getColor(getContext(), com.example.expenseeye.theme.ThemeManager.ThemeColor.PRIMARY));
            }

            layoutBudgetContainer.addView(budgetView);
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
        ExpenseDialogHelper.showExpenseDialog(requireContext(), getLayoutInflater(), viewModel, null, null, null);
    }

    private void showEditExpenseBottomSheet(Expense expense) {
        ExpenseDialogHelper.showExpenseDialog(requireContext(), getLayoutInflater(), viewModel, expense, null, null);
    }

    private void updateDebtContainerVisibility() {
        if (layoutDebtContainer == null) return;
        if (totalOwedToOthers > 0 || totalOwedToMe > 0) {
            layoutDebtContainer.setVisibility(View.VISIBLE);
        } else {
            layoutDebtContainer.setVisibility(View.GONE);
        }
    }

    private void updateGreetingText() {
        if (tvGreeting == null) return;
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good morning";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good afternoon";
        } else if (hour >= 17 && hour < 21) {
            greeting = "Good evening";
        } else {
            greeting = "Good night";
        }
        tvGreeting.setText(greeting + ", Alex");
    }

    private void updateGreetingSubtext() {
        if (tvGreetingSub == null) return;
        String[] variations = {
            "Here's a look at your finances today.",
            "Let's review your spending behavior today.",
            "Here is a snapshot of your budget status.",
            "Track your expenses and grow your savings today.",
            "Let's see how your budget is holding up today.",
            "Keep your eyes on your financial goals today."
        };
        int randomIndex = new java.util.Random().nextInt(variations.length);
        tvGreetingSub.setText(variations[randomIndex]);
    }

    @Override
    public void onResume() {
        super.onResume();
        greetingRunnable = new Runnable() {
            @Override
            public void run() {
                updateGreetingText();
                greetingHandler.postDelayed(this, 60000);
            }
        };
        greetingHandler.post(greetingRunnable);
        updateGreetingSubtext();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (greetingRunnable != null) {
            greetingHandler.removeCallbacks(greetingRunnable);
        }
    }
}
