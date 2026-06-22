package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expenseeye.models.ChecklistItem;

import java.util.List;

@Dao
public interface ChecklistItemDao {
    @Query("SELECT * FROM checklist_items ORDER BY isCompleted ASC, id DESC")
    LiveData<List<ChecklistItem>> getAllChecklistItems();

    @Query("SELECT * FROM checklist_items ORDER BY isCompleted ASC, id DESC")
    List<ChecklistItem> getAllChecklistItemsSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ChecklistItem item);

    @Update
    void update(ChecklistItem item);

    @Delete
    void delete(ChecklistItem item);

    @Query("SELECT COUNT(*) FROM checklist_items WHERE isCompleted = 0")
    LiveData<Integer> getPendingCountLive();
}
