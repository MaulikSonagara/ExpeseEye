package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expenseeye.models.Category;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<Category> getAllCategoriesSync();

    @Query("SELECT * FROM categories WHERE is_enabled = 1 ORDER BY name ASC")
    LiveData<List<Category>> getEnabledCategories();

    @Query("SELECT * FROM categories WHERE is_enabled = 1 ORDER BY name ASC")
    List<Category> getEnabledCategoriesSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    Category getByName(String name);

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    Category getById(int id);

    @Query("SELECT COUNT(*) FROM categories")
    int getCount();

    @Query("DELETE FROM categories")
    void deleteAll();
}
