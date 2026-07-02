package com.example.expenseeye.repository;

import android.app.Application;
import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;

import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.database.BudgetDao;
import com.example.expenseeye.database.CategoryDao;
import com.example.expenseeye.database.CategoryKeywordsDao;
import com.example.expenseeye.database.ChecklistItemDao;
import com.example.expenseeye.database.ExpenseDao;
import com.example.expenseeye.database.PaymentMethodDao;
import com.example.expenseeye.database.ReminderExpenseDao;
import com.example.expenseeye.database.TripDao;
import com.example.expenseeye.models.Budget;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.CategoryKeyword;
import com.example.expenseeye.models.ChecklistItem;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.models.ReminderExpense;
import com.example.expenseeye.models.BorrowOwe;
import com.example.expenseeye.models.BorrowOwePayment;
import com.example.expenseeye.models.Trip;
import com.example.expenseeye.database.BorrowOweDao;
import com.example.expenseeye.database.BorrowOwePaymentDao;
import com.example.expenseeye.utils.AlarmScheduler;
import com.example.expenseeye.widget.WidgetProvider;

import java.util.ArrayList;
import java.util.List;

public class AppRepository {
    private final Context context;
    private final ExpenseDao expenseDao;
    private final CategoryDao categoryDao;
    private final PaymentMethodDao paymentMethodDao;
    private final ChecklistItemDao checklistItemDao;
    private final CategoryKeywordsDao categoryKeywordsDao;
    private final BudgetDao budgetDao;
    private final ReminderExpenseDao reminderExpenseDao;
    private final TripDao tripDao;

    private final LiveData<List<Expense>> allExpenses;
    private final LiveData<List<Category>> allCategories;
    private final LiveData<List<PaymentMethod>> allPaymentMethods;
    private final LiveData<List<ChecklistItem>> allChecklistItems;
    private final LiveData<List<CategoryKeyword>> allKeywords;
    private final BorrowOweDao borrowOweDao;
    private final LiveData<List<BorrowOwe>> allBorrowOwes;
    private final BorrowOwePaymentDao borrowOwePaymentDao;

    public AppRepository(Application application) {
        this.context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(application);
        expenseDao = db.expenseDao();
        categoryDao = db.categoryDao();
        paymentMethodDao = db.paymentMethodDao();
        checklistItemDao = db.checklistItemDao();
        categoryKeywordsDao = db.categoryKeywordsDao();
        budgetDao = db.budgetDao();
        reminderExpenseDao = db.reminderExpenseDao();
        tripDao = db.tripDao();

        allExpenses = expenseDao.getAllExpenses();
        allCategories = categoryDao.getAllCategories();
        allPaymentMethods = paymentMethodDao.getAllPaymentMethods();
        allChecklistItems = checklistItemDao.getAllChecklistItems();
        allKeywords = categoryKeywordsDao.getAllKeywords();
        borrowOweDao = db.borrowOweDao();
        allBorrowOwes = borrowOweDao.getAllItems();
        borrowOwePaymentDao = db.borrowOwePaymentDao();
    }

    // Expense operations
    public LiveData<List<Expense>> getAllExpenses() {
        return allExpenses;
    }

    public List<Expense> getAllExpensesSync() {
        return expenseDao.getAllExpensesSync();
    }

    public void insertExpense(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expenseDao.insert(expense);
            WidgetProvider.updateAllWidgets(context);
        });
    }

    public void updateExpense(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expenseDao.update(expense);
            WidgetProvider.updateAllWidgets(context);
        });
    }

    public void deleteExpense(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expenseDao.delete(expense);
            WidgetProvider.updateAllWidgets(context);
        });
    }

    public Expense findExpenseForBorrowOweSync(long timestamp) {
        long minTime = timestamp - 5000;
        long maxTime = timestamp + 5000;
        return expenseDao.findExpenseForBorrowOwe(minTime, maxTime);
    }

    public LiveData<Double> getTotalSpendingInRangeLive(long start, long end) {
        return expenseDao.getTotalSpendingInRangeLive(start, end);
    }

    public LiveData<List<Expense>> getExpensesInRange(long start, long end) {
        return expenseDao.getExpensesInRange(start, end);
    }

    public LiveData<List<Expense>> getFilteredExpenses(
            String searchQuery,
            Long startDate,
            Long endDate,
            List<String> categories,
            List<String> paymentMethods,
            Double minAmount,
            Double maxAmount
    ) {
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM expenses WHERE 1=1");
        List<Object> args = new ArrayList<>();

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            queryBuilder.append(" AND (title LIKE ? OR description LIKE ?)");
            String likeQuery = "%" + searchQuery.trim() + "%";
            args.add(likeQuery);
            args.add(likeQuery);
        }

        if (startDate != null) {
            queryBuilder.append(" AND timestamp >= ?");
            args.add(startDate);
        }

        if (endDate != null) {
            queryBuilder.append(" AND timestamp <= ?");
            args.add(endDate);
        }

        if (categories != null && !categories.isEmpty()) {
            queryBuilder.append(" AND categoryName IN (");
            for (int i = 0; i < categories.size(); i++) {
                queryBuilder.append("?");
                if (i < categories.size() - 1) {
                    queryBuilder.append(",");
                }
                args.add(categories.get(i));
            }
            queryBuilder.append(")");
        }

        if (paymentMethods != null && !paymentMethods.isEmpty()) {
            queryBuilder.append(" AND paymentMethodName IN (");
            for (int i = 0; i < paymentMethods.size(); i++) {
                queryBuilder.append("?");
                if (i < paymentMethods.size() - 1) {
                    queryBuilder.append(",");
                }
                args.add(paymentMethods.get(i));
            }
            queryBuilder.append(")");
        }

        if (minAmount != null) {
            queryBuilder.append(" AND amount >= ?");
            args.add(minAmount);
        }

        if (maxAmount != null) {
            queryBuilder.append(" AND amount <= ?");
            args.add(maxAmount);
        }

        queryBuilder.append(" ORDER BY timestamp DESC");

        SimpleSQLiteQuery rawQuery = new SimpleSQLiteQuery(queryBuilder.toString(), args.toArray());
        return expenseDao.getFilteredExpenses(rawQuery);
    }

    // Category operations
    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public List<Category> getAllCategoriesSync() {
        return categoryDao.getAllCategoriesSync();
    }

    public LiveData<List<Category>> getEnabledCategories() {
        return categoryDao.getEnabledCategories();
    }

    public List<Category> getEnabledCategoriesSync() {
        return categoryDao.getEnabledCategoriesSync();
    }

    public void insertCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.insert(category));
    }

    public void updateCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.update(category));
    }

    public void deleteCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.delete(category));
    }

    // Category Keywords operations
    public LiveData<List<CategoryKeyword>> getKeywordsForCategory(int categoryId) {
        return categoryKeywordsDao.getKeywordsForCategory(categoryId);
    }

    public List<CategoryKeyword> getKeywordsForCategorySync(int categoryId) {
        return categoryKeywordsDao.getKeywordsForCategorySync(categoryId);
    }

    public LiveData<List<CategoryKeyword>> getAllKeywords() {
        return allKeywords;
    }

    public List<CategoryKeyword> getAllKeywordsSync() {
        return categoryKeywordsDao.getAllKeywordsSync();
    }

    public void insertCategoryKeyword(CategoryKeyword keyword) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryKeywordsDao.insert(keyword));
    }

    public void deleteCategoryKeyword(CategoryKeyword keyword) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryKeywordsDao.delete(keyword));
    }

    public void deleteKeywordsForCategory(int categoryId) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryKeywordsDao.deleteKeywordsForCategory(categoryId));
    }

    public void deleteCategoryAndMoveExpenses(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Category others = categoryDao.getByName("Others");
            if (others == null) {
                others = categoryDao.getByName("Other");
            }
            int othersId = 0;
            String othersName = "Others";
            if (others != null) {
                othersId = others.getId();
                othersName = others.getName();
            } else {
                Category newOthers = new Category("Others", "ic_other", android.graphics.Color.parseColor("#9E9E9E"), true);
                othersId = (int) categoryDao.insert(newOthers);
                othersName = "Others";
            }
            expenseDao.updateExpenseCategory(category.getId(), othersId, othersName);
            categoryDao.delete(category);
        });
    }

    public void resetCategoriesToDefault() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Category othersCat = categoryDao.getByName("Others");
            if (othersCat == null) {
                othersCat = categoryDao.getByName("Other");
            }
            int othersId = 0;
            String othersName = "Others";
            if (othersCat != null) {
                othersId = othersCat.getId();
                othersName = othersCat.getName();
            }

            List<Category> allCurrent = categoryDao.getAllCategoriesSync();

            String[] defaultNames = {"Food", "Travel", "Shopping", "Bills", "Health", "Entertainment", "Salary", "Investment", "Education", "Others"};
            String[] defaultIcons = {"ic_food", "ic_travel", "ic_shopping", "ic_bills", "ic_medical", "ic_entertainment", "ic_bank", "ic_card", "ic_education", "ic_other"};
            String[] defaultColors = {"#FF5722", "#4CAF50", "#E91E63", "#FF9800", "#009688", "#9C27B0", "#2196F3", "#3F51B5", "#795548", "#9E9E9E"};

            String[][] defaultKeywords = {
                {"food", "breakfast", "lunch", "dinner", "nasta", "nashta", "samosa", "kachori", "fafda", "jalebi", "chai", "tea", "coffee", "cafe", "pizza", "burger", "sandwich", "dosa", "idli", "vada", "pav bhaji", "vadapav", "bhel", "panipuri", "maggi", "ramen", "groceries", "swiggy", "zomato", "restaurant", "hotel", "bakery", "snacks", "juice", "cold drink"},
                {"travel", "trip", "fuel", "petrol", "diesel", "cng", "uber", "ola", "auto", "taxi", "bus", "train", "flight", "booking", "hotel", "hostel", "vacation", "holiday", "toll", "parking", "luggage", "ticket"},
                {"shopping", "clothes", "shoes", "fashion", "amazon", "flipkart", "myntra", "ajio", "meesho", "accessories", "electronics", "gadget", "mobile", "laptop", "watch", "gift", "furniture", "home decor", "grocery shopping"},
                {"electricity", "light bill", "power bill", "gas bill", "water bill", "internet", "wifi", "broadband", "recharge", "mobile bill", "postpaid", "prepaid", "dth", "cable", "rent", "emi", "subscription", "netflix", "hotstar", "prime", "spotify", "maintenance", "society bill"},
                {"doctor", "hospital", "clinic", "medicine", "pharmacy", "medical", "tablet", "injection", "surgery", "test", "blood test", "checkup", "dentist", "eye care", "health insurance", "gym", "protein", "supplement"},
                {"movie", "cinema", "game", "gaming", "steam", "psn", "xbox", "netflix", "hotstar", "amazon prime", "spotify", "party", "club", "concert", "event", "picnic", "amusement park", "cricket match"},
                {"salary", "income", "paycheck", "stipend", "freelance", "bonus", "incentive", "payout", "profit", "wage", "monthly income", "commission"},
                {"investment", "stock", "share", "mutual fund", "sip", "fd", "rd", "crypto", "bitcoin", "gold", "silver", "trading", "demat", "zerodha", "groww", "upstox", "angel one", "bond", "equity", "dividend"},
                {"school", "college", "fees", "tuition", "coaching", "books", "notebook", "stationery", "exam fee", "course", "udemy", "coursera", "class", "workshop", "seminar", "project", "training", "hostel fee"},
                {"miscellaneous", "other", "random", "unknown", "general", "extra", "uncategorized"}
            };

            if (othersCat == null) {
                othersCat = new Category("Others", "ic_other", android.graphics.Color.parseColor("#9E9E9E"), true);
                othersId = (int) categoryDao.insert(othersCat);
                othersName = "Others";
            }

            for (Category c : allCurrent) {
                boolean isDefault = false;
                for (String defName : defaultNames) {
                    if (c.getName().equalsIgnoreCase(defName)) {
                        isDefault = true;
                        break;
                    }
                }
                if (!isDefault && c.getId() != othersId) {
                    expenseDao.updateExpenseCategory(c.getId(), othersId, othersName);
                    categoryDao.delete(c);
                }
            }

            for (int i = 0; i < defaultNames.length; i++) {
                Category cat = categoryDao.getByName(defaultNames[i]);
                int catId;
                if (cat == null) {
                    Category newCat = new Category(defaultNames[i], defaultIcons[i], android.graphics.Color.parseColor(defaultColors[i]), true);
                    catId = (int) categoryDao.insert(newCat);
                } else {
                    cat.setIconName(defaultIcons[i]);
                    cat.setColor(android.graphics.Color.parseColor(defaultColors[i]));
                    cat.setDefault(true);
                    cat.setEnabled(true);
                    categoryDao.update(cat);
                    catId = cat.getId();
                }

                categoryKeywordsDao.deleteKeywordsForCategory(catId);
                for (String kw : defaultKeywords[i]) {
                    categoryKeywordsDao.insert(new CategoryKeyword(catId, kw));
                }
            }
        });
    }

    // PaymentMethod operations
    public LiveData<List<PaymentMethod>> getAllPaymentMethods() {
        return allPaymentMethods;
    }

    public List<PaymentMethod> getAllPaymentMethodsSync() {
        return paymentMethodDao.getAllPaymentMethodsSync();
    }

    public void insertPaymentMethod(PaymentMethod paymentMethod) {
        AppDatabase.databaseWriteExecutor.execute(() -> paymentMethodDao.insert(paymentMethod));
    }

    public void updatePaymentMethod(PaymentMethod paymentMethod) {
        AppDatabase.databaseWriteExecutor.execute(() -> paymentMethodDao.update(paymentMethod));
    }

    public void deletePaymentMethod(PaymentMethod paymentMethod) {
        AppDatabase.databaseWriteExecutor.execute(() -> paymentMethodDao.delete(paymentMethod));
    }

    // Checklist operations
    public LiveData<List<ChecklistItem>> getAllChecklistItems() {
        return allChecklistItems;
    }

    public List<ChecklistItem> getAllChecklistItemsSync() {
        return checklistItemDao.getAllChecklistItemsSync();
    }

    public void insertChecklistItem(ChecklistItem item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            checklistItemDao.insert(item);
            WidgetProvider.updateAllWidgets(context);
        });
    }

    public void updateChecklistItem(ChecklistItem item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            checklistItemDao.update(item);
            WidgetProvider.updateAllWidgets(context);
        });
    }

    public void deleteChecklistItem(ChecklistItem item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            checklistItemDao.delete(item);
            WidgetProvider.updateAllWidgets(context);
        });
    }

    public LiveData<Integer> getPendingChecklistCountLive() {
        return checklistItemDao.getPendingCountLive();
    }

    // Budget operations
    public LiveData<List<Budget>> getBudgetsForMonth(String month) {
        return budgetDao.getBudgetsForMonth(month);
    }

    public LiveData<Integer> getBudgetCountLive(String month) {
        return budgetDao.getBudgetCountLive(month);
    }

    public void insertBudget(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> budgetDao.insert(budget));
    }

    public void updateBudget(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> budgetDao.update(budget));
    }

    public void deleteBudget(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> budgetDao.delete(budget));
    }

    public Budget getBudgetSync(String month, String category) {
        return budgetDao.getBudgetSync(month, category);
    }

    // Reminder Expense operations
    public LiveData<List<ReminderExpense>> getAllReminderExpenses() {
        return reminderExpenseDao.getAllReminderExpenses();
    }

    public LiveData<Integer> getReminderExpenseCountLive() {
        return reminderExpenseDao.getReminderExpenseCountLive();
    }

    public void insertReminderExpense(ReminderExpense reminderExpense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = reminderExpenseDao.insert(reminderExpense);
            if (reminderExpense.isEnabled() && reminderExpense.getNextDueTimestamp() > System.currentTimeMillis()) {
                AlarmScheduler.scheduleOneTime(
                        context,
                        (int) id,
                        "Reminder: " + reminderExpense.getTitle(),
                        "Your scheduled expense is due.",
                        reminderExpense.getNextDueTimestamp()
                );
            }
        });
    }

    public void updateReminderExpense(ReminderExpense reminderExpense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            reminderExpenseDao.update(reminderExpense);
            if (reminderExpense.isEnabled()) {
                if (reminderExpense.getNextDueTimestamp() > System.currentTimeMillis()) {
                    AlarmScheduler.scheduleOneTime(
                            context,
                            reminderExpense.getId(),
                            "Reminder: " + reminderExpense.getTitle(),
                            "Your scheduled expense is due.",
                            reminderExpense.getNextDueTimestamp()
                    );
                }
            } else {
                AlarmScheduler.cancelAlarm(context, reminderExpense.getId());
            }
        });
    }

    public void deleteReminderExpense(ReminderExpense reminderExpense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            reminderExpenseDao.delete(reminderExpense);
            AlarmScheduler.cancelAlarm(context, reminderExpense.getId());
        });
    }

    // Borrow/Owe operations
    public LiveData<List<BorrowOwe>> getAllBorrowOwes() {
        return allBorrowOwes;
    }

    public LiveData<Double> getTotalOwedToOthers() {
        return borrowOweDao.getTotalOwedToOthers();
    }

    public LiveData<Double> getTotalOwedToMe() {
        return borrowOweDao.getTotalOwedToMe();
    }

    public void insertBorrowOwe(BorrowOwe item) {
        AppDatabase.databaseWriteExecutor.execute(() -> borrowOweDao.insert(item));
    }

    public void updateBorrowOwe(BorrowOwe item) {
        AppDatabase.databaseWriteExecutor.execute(() -> borrowOweDao.update(item));
    }

    public void deleteBorrowOwe(BorrowOwe item) {
        AppDatabase.databaseWriteExecutor.execute(() -> borrowOweDao.delete(item));
    }

    public LiveData<BorrowOwe> getBorrowOweById(long id) {
        return borrowOweDao.getItemById(id);
    }

    // Borrow/Owe Payment operations
    public LiveData<List<BorrowOwePayment>> getPaymentsForBorrowOwe(long borrowOweId) {
        return borrowOwePaymentDao.getPaymentsForBorrowOwe(borrowOweId);
    }

    public LiveData<Double> getTotalPaidForBorrowOwe(long borrowOweId) {
        return borrowOwePaymentDao.getTotalPaidForBorrowOwe(borrowOweId);
    }

    public void insertBorrowOwePayment(BorrowOwePayment payment) {
        AppDatabase.databaseWriteExecutor.execute(() -> borrowOwePaymentDao.insert(payment));
    }

    public void deleteBorrowOwePayment(BorrowOwePayment payment) {
        AppDatabase.databaseWriteExecutor.execute(() -> borrowOwePaymentDao.delete(payment));
    }

    // --- Trip Methods ---

    public LiveData<List<Trip>> getAllTrips() {
        return tripDao.getAllTrips();
    }

    public List<Trip> getAllTripsSync() {
        // I need to add this to TripDao too if not there
        return tripDao.getAllTripsSync();
    }

    public LiveData<Trip> getActiveTrip() {
        return tripDao.getActiveTrip();
    }

    public void insertTrip(Trip trip) {
        AppDatabase.databaseWriteExecutor.execute(() -> tripDao.insert(trip));
    }

    public void updateTrip(Trip trip) {
        AppDatabase.databaseWriteExecutor.execute(() -> tripDao.update(trip));
    }

    public void deleteTrip(Trip trip) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expenseDao.unlinkExpensesFromTrip(trip.getId());
            tripDao.delete(trip);
        });
    }

    public void activateTrip(int tripId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            tripDao.deactivateAllTrips();
            tripDao.activateTrip(tripId);
        });
    }

    public void deactivateAllTrips() {
        AppDatabase.databaseWriteExecutor.execute(() -> tripDao.deactivateAllTrips());
    }

    public LiveData<Trip> getTripById(int tripId) {
        return tripDao.getTripById(tripId);
    }

    public Trip getActiveTripSync() {
        return tripDao.getActiveTripSync();
    }

    public LiveData<List<Expense>> getExpensesForTrip(int tripId) {
        return expenseDao.getExpensesForTrip(tripId);
    }

    public List<Expense> getExpensesForTripSync(int tripId) {
        return expenseDao.getExpensesForTripSync(tripId);
    }

    public void linkExpensesToTrip(List<Long> expenseIds, int tripId) {
        AppDatabase.databaseWriteExecutor.execute(() -> expenseDao.linkExpensesToTrip(expenseIds, tripId));
    }
}
