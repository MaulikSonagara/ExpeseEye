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

    @Query("UPDATE expenses SET categoryId = :newId, categoryName = :newName WHERE categoryId = :oldId")
    void updateExpenseCategory(int oldId, int newId, String newName);

    @Query("SELECT * FROM expenses WHERE description = 'Borrow/Owe Reference' AND timestamp >= :minTime AND timestamp <= :maxTime LIMIT 1")
    Expense findExpenseForBorrowOwe(long minTime, long maxTime);

    @Query("SELECT * FROM expenses WHERE trip_id = :tripId ORDER BY timestamp DESC")
    List<Expense> getExpensesForTripSync(int tripId);

    @Query("SELECT * FROM expenses WHERE trip_id = :tripId ORDER BY timestamp DESC")
    LiveData<List<Expense>> getExpensesForTrip(int tripId);

    @Query("UPDATE expenses SET trip_id = :tripId WHERE id IN (:expenseIds)")
    void linkExpensesToTrip(List<Long> expenseIds, int tripId);

    @Query("UPDATE expenses SET trip_id = -1 WHERE trip_id = :tripId")
    void unlinkExpensesFromTrip(int tripId);
}
