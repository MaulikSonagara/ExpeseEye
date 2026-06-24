package com.example.expenseeye.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(
    tableName = "category_keywords",
    foreignKeys = @ForeignKey(
        entity = Category.class,
        parentColumns = "id",
        childColumns = "category_id",
        onDelete = ForeignKey.CASCADE
    )
)
public class CategoryKeyword {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "category_id")
    private int categoryId;

    private String keyword;

    public CategoryKeyword(int categoryId, String keyword) {
        this.categoryId = categoryId;
        this.keyword = keyword;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
