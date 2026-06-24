package com.example.expenseeye.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.CategoryKeyword;
import com.example.expenseeye.models.ChecklistItem;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.repository.AppRepository;

import java.util.ArrayList;
import java.util.List;

public class AppViewModel extends AndroidViewModel {
    private final AppRepository repository;

    private final LiveData<List<Expense>> allExpenses;
    private final LiveData<List<Category>> allCategories;
    private final LiveData<List<PaymentMethod>> allPaymentMethods;
    private final LiveData<List<ChecklistItem>> allChecklistItems;
    private final LiveData<List<CategoryKeyword>> allKeywords;
    private final LiveData<Integer> pendingChecklistCount;

    // Filter states
    public static class FilterParams {
        public String searchQuery = "";
        public Long startDate = null;
        public Long endDate = null;
        public List<String> categories = new ArrayList<>();
        public List<String> paymentMethods = new ArrayList<>();
        public Double minAmount = null;
        public Double maxAmount = null;
    }

    private final MutableLiveData<FilterParams> filterParams = new MutableLiveData<>(new FilterParams());
    private final LiveData<List<Expense>> filteredExpenses;

    public AppViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);

        allExpenses = repository.getAllExpenses();
        allCategories = repository.getAllCategories();
        allPaymentMethods = repository.getAllPaymentMethods();
        allChecklistItems = repository.getAllChecklistItems();
        allKeywords = repository.getAllKeywords();
        pendingChecklistCount = repository.getPendingChecklistCountLive();

        // Reactive filter switchmap
        filteredExpenses = Transformations.switchMap(filterParams, params ->
                repository.getFilteredExpenses(
                        params.searchQuery,
                        params.startDate,
                        params.endDate,
                        params.categories,
                        params.paymentMethods,
                        params.minAmount,
                        params.maxAmount
                )
        );
    }

    // Getters
    public LiveData<List<Expense>> getAllExpenses() {
        return allExpenses;
    }

    public List<Expense> getAllExpensesSync() {
        return repository.getAllExpensesSync();
    }

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public List<Category> getAllCategoriesSync() {
        return repository.getAllCategoriesSync();
    }

    public LiveData<List<PaymentMethod>> getAllPaymentMethods() {
        return allPaymentMethods;
    }

    public List<PaymentMethod> getAllPaymentMethodsSync() {
        return repository.getAllPaymentMethodsSync();
    }

    public LiveData<List<ChecklistItem>> getAllChecklistItems() {
        return allChecklistItems;
    }

    public List<ChecklistItem> getAllChecklistItemsSync() {
        return repository.getAllChecklistItemsSync();
    }

    public LiveData<Integer> getPendingChecklistCount() {
        return pendingChecklistCount;
    }

    public LiveData<List<Expense>> getFilteredExpenses() {
        return filteredExpenses;
    }

    // Filter controls
    public void updateFilters(FilterParams params) {
        filterParams.setValue(params);
    }

    public FilterParams getFilterParams() {
        return filterParams.getValue();
    }

    // Expense actions
    public void insertExpense(Expense expense) {
        repository.insertExpense(expense);
    }

    public void updateExpense(Expense expense) {
        repository.updateExpense(expense);
    }

    public void deleteExpense(Expense expense) {
        repository.deleteExpense(expense);
    }

    // Category actions
    public void insertCategory(Category category) {
        repository.insertCategory(category);
    }

    public void updateCategory(Category category) {
        repository.updateCategory(category);
    }

    public void deleteCategory(Category category) {
        repository.deleteCategory(category);
    }

    public LiveData<List<Category>> getEnabledCategories() {
        return repository.getEnabledCategories();
    }

    public List<Category> getEnabledCategoriesSync() {
        return repository.getEnabledCategoriesSync();
    }

    public LiveData<List<CategoryKeyword>> getKeywordsForCategory(int categoryId) {
        return repository.getKeywordsForCategory(categoryId);
    }

    public List<CategoryKeyword> getKeywordsForCategorySync(int categoryId) {
        return repository.getKeywordsForCategorySync(categoryId);
    }

    public LiveData<List<CategoryKeyword>> getAllKeywords() {
        return repository.getAllKeywords();
    }

    public List<CategoryKeyword> getAllKeywordsSync() {
        return repository.getAllKeywordsSync();
    }

    public void insertCategoryKeyword(CategoryKeyword keyword) {
        repository.insertCategoryKeyword(keyword);
    }

    public void deleteCategoryKeyword(CategoryKeyword keyword) {
        repository.deleteCategoryKeyword(keyword);
    }

    public void deleteKeywordsForCategory(int categoryId) {
        repository.deleteKeywordsForCategory(categoryId);
    }

    public void deleteCategoryAndMoveExpenses(Category category) {
        repository.deleteCategoryAndMoveExpenses(category);
    }

    public void resetCategoriesToDefault() {
        repository.resetCategoriesToDefault();
    }

    // PaymentMethod actions
    public void insertPaymentMethod(PaymentMethod pm) {
        repository.insertPaymentMethod(pm);
    }

    public void updatePaymentMethod(PaymentMethod pm) {
        repository.updatePaymentMethod(pm);
    }

    public void deletePaymentMethod(PaymentMethod pm) {
        repository.deletePaymentMethod(pm);
    }

    // Checklist actions
    public void insertChecklistItem(ChecklistItem item) {
        repository.insertChecklistItem(item);
    }

    public void updateChecklistItem(ChecklistItem item) {
        repository.updateChecklistItem(item);
    }

    public void deleteChecklistItem(ChecklistItem item) {
        repository.deleteChecklistItem(item);
    }

    // Stats
    public LiveData<Double> getTotalSpendingInRange(long start, long end) {
        return repository.getTotalSpendingInRangeLive(start, end);
    }

    public LiveData<List<Expense>> getExpensesInRange(long start, long end) {
        return repository.getExpensesInRange(start, end);
    }
}
