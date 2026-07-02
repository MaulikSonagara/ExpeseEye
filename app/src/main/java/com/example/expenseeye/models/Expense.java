package com.example.expenseeye.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class Expense {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String description;
    private double amount;
    private long timestamp;
    private int categoryId;
    private String categoryName;
    private int paymentMethodId;
    private String paymentMethodName;
    @androidx.room.ColumnInfo(name = "type", defaultValue = "0")
    private int type; // 0 for Expense, 1 for Income
    @androidx.room.ColumnInfo(name = "trip_id", defaultValue = "-1")
    private int tripId; // -1 for no trip

    public Expense(String title, String description, double amount, long timestamp, int categoryId, String categoryName, int paymentMethodId, String paymentMethodName, int type) {
        this(title, description, amount, timestamp, categoryId, categoryName, paymentMethodId, paymentMethodName, type, -1);
    }

    @androidx.room.Ignore
    public Expense(String title, String description, double amount, long timestamp, int categoryId, String categoryName, int paymentMethodId, String paymentMethodName, int type, int tripId) {
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.timestamp = timestamp;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.paymentMethodId = paymentMethodId;
        this.paymentMethodName = paymentMethodName;
        this.type = type;
        this.tripId = tripId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(int paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getPaymentMethodName() {
        return paymentMethodName;
    }

    public void setPaymentMethodName(String paymentMethodName) {
        this.paymentMethodName = paymentMethodName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }
}
