package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expenseeye.models.BorrowOwe;

import java.util.List;

@Dao
public interface BorrowOweDao {
    @Insert
    long insert(BorrowOwe item);

    @Update
    void update(BorrowOwe item);

    @Delete
    void delete(BorrowOwe item);

    @Query("SELECT * FROM borrow_owe ORDER BY timestamp DESC")
    LiveData<List<BorrowOwe>> getAllItems();

    @Query("SELECT * FROM borrow_owe ORDER BY timestamp DESC")
    List<BorrowOwe> getAllItemsSync();

    @Query("SELECT * FROM borrow_owe WHERE id = :id")
    BorrowOwe getItemByIdSync(long id);

    @Query("SELECT * FROM borrow_owe WHERE id = :id")
    LiveData<BorrowOwe> getItemById(long id);

    @Query("SELECT SUM(amount) FROM borrow_owe WHERE isBorrow = 1 AND isSettled = 0")
    LiveData<Double> getTotalOwedToOthers(); // Owe

    @Query("SELECT SUM(amount) FROM borrow_owe WHERE isBorrow = 0 AND isSettled = 0")
    LiveData<Double> getTotalOwedToMe(); // Borrowed by others (Lent)
}
