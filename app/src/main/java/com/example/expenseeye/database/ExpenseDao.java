package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.example.expenseeye.models.Expense;

import java.util.List;

@Dao
public interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    LiveData<List<Expense>> getAllExpenses();

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    List<Expense> getAllExpensesSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    @RawQuery(observedEntities = Expense.class)
    LiveData<List<Expense>> getFilteredExpenses(SupportSQLiteQuery query);

    @RawQuery
    List<Expense> getFilteredExpensesSync(SupportSQLiteQuery query);

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    double getTotalSpendingInRange(long startTimestamp, long endTimestamp);

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    LiveData<Double> getTotalSpendingInRangeLive(long startTimestamp, long endTimestamp);

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp DESC")
    LiveData<List<Expense>> getExpensesInRange(long startTimestamp, long endTimestamp);

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp DESC")
    List<Expense> getExpensesInRangeSync(long startTimestamp, long endTimestamp);
}
