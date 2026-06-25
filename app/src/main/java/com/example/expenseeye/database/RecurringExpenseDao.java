package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expenseeye.models.RecurringExpense;

import java.util.List;

@Dao
public interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses WHERE isEnabled = 1")
    LiveData<List<RecurringExpense>> getActiveRecurringExpenses();

    @Query("SELECT * FROM recurring_expenses")
    LiveData<List<RecurringExpense>> getAllRecurringExpenses();

    @Query("SELECT * FROM recurring_expenses WHERE isEnabled = 1")
    List<RecurringExpense> getActiveRecurringExpensesSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RecurringExpense recurringExpense);

    @Update
    void update(RecurringExpense recurringExpense);

    @Delete
    void delete(RecurringExpense recurringExpense);
}
