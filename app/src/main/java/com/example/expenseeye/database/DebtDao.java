package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expenseeye.models.Debt;

import java.util.List;

@Dao
public interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY isResolved ASC, dueDate ASC")
    LiveData<List<Debt>> getAllDebts();

    @Query("SELECT * FROM debts ORDER BY isResolved ASC, dueDate ASC")
    List<Debt> getAllDebtsSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Debt debt);

    @Update
    void update(Debt debt);

    @Delete
    void delete(Debt debt);

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    Debt getByIdSync(int id);
}
