package com.example.expenseeye.repository;

import android.app.Application;
import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;

import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.database.CategoryDao;
import com.example.expenseeye.database.ChecklistItemDao;
import com.example.expenseeye.database.ExpenseDao;
import com.example.expenseeye.database.PaymentMethodDao;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.ChecklistItem;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.widget.WidgetProvider;

import java.util.ArrayList;
import java.util.List;

public class AppRepository {
    private final Context context;
    private final ExpenseDao expenseDao;
    private final CategoryDao categoryDao;
    private final PaymentMethodDao paymentMethodDao;
    private final ChecklistItemDao checklistItemDao;

    private final LiveData<List<Expense>> allExpenses;
    private final LiveData<List<Category>> allCategories;
    private final LiveData<List<PaymentMethod>> allPaymentMethods;
    private final LiveData<List<ChecklistItem>> allChecklistItems;

    public AppRepository(Application application) {
        this.context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(application);
        expenseDao = db.expenseDao();
        categoryDao = db.categoryDao();
        paymentMethodDao = db.paymentMethodDao();
        checklistItemDao = db.checklistItemDao();

        allExpenses = expenseDao.getAllExpenses();
        allCategories = categoryDao.getAllCategories();
        allPaymentMethods = paymentMethodDao.getAllPaymentMethods();
        allChecklistItems = checklistItemDao.getAllChecklistItems();
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

    public void insertCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.insert(category));
    }

    public void updateCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.update(category));
    }

    public void deleteCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.delete(category));
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
        AppDatabase.databaseWriteExecutor.execute(() -> checklistItemDao.insert(item));
    }

    public void updateChecklistItem(ChecklistItem item) {
        AppDatabase.databaseWriteExecutor.execute(() -> checklistItemDao.update(item));
    }

    public void deleteChecklistItem(ChecklistItem item) {
        AppDatabase.databaseWriteExecutor.execute(() -> checklistItemDao.delete(item));
    }

    public LiveData<Integer> getPendingChecklistCountLive() {
        return checklistItemDao.getPendingCountLive();
    }
}
