package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expenseeye.models.Budget;

import java.util.List;

@Dao
public interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month")
    LiveData<List<Budget>> getBudgetsForMonth(String month);

    @Query("SELECT * FROM budgets WHERE month = :month AND categoryName = :categoryName LIMIT 1")
    Budget getBudgetSync(String month, String categoryName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Budget budget);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    @Query("DELETE FROM budgets")
    void deleteAll();
}
