package com.example.expenseeye.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "checklist_items")
public class ChecklistItem {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String category;
    private String quantity;
    private String priority;
    private boolean isCompleted;

    public ChecklistItem(String title, String category, String quantity, String priority, boolean isCompleted) {
        this.title = title;
        this.category = category;
        this.quantity = quantity;
        this.priority = priority;
        this.isCompleted = isCompleted;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
