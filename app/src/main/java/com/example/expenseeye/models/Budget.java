package com.example.expenseeye.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class Budget {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private double amount;
    private String categoryName; // "Overall" for total budget
    private String month; // MM-yyyy format

    public Budget(double amount, String categoryName, String month) {
        this.amount = amount;
        this.categoryName = categoryName;
        this.month = month;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }
}
