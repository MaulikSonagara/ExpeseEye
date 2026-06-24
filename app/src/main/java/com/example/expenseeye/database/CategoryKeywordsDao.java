package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.expenseeye.models.CategoryKeyword;
import java.util.List;

@Dao
public interface CategoryKeywordsDao {
    @Query("SELECT * FROM category_keywords WHERE category_id = :categoryId ORDER BY keyword ASC")
    LiveData<List<CategoryKeyword>> getKeywordsForCategory(int categoryId);

    @Query("SELECT * FROM category_keywords WHERE category_id = :categoryId ORDER BY keyword ASC")
    List<CategoryKeyword> getKeywordsForCategorySync(int categoryId);

    @Query("SELECT * FROM category_keywords ORDER BY keyword ASC")
    LiveData<List<CategoryKeyword>> getAllKeywords();

    @Query("SELECT * FROM category_keywords ORDER BY keyword ASC")
    List<CategoryKeyword> getAllKeywordsSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CategoryKeyword keyword);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CategoryKeyword> keywords);

    @Delete
    void delete(CategoryKeyword keyword);

    @Query("DELETE FROM category_keywords WHERE category_id = :categoryId")
    void deleteKeywordsForCategory(int categoryId);

    @Query("DELETE FROM category_keywords")
    void deleteAll();
}
