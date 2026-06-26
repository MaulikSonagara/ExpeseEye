package com.example.expenseeye.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminder_expenses")
public class ReminderExpense {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private double amount;
    private int categoryId;
    private String categoryName;
    private int paymentMethodId;
    private String paymentMethodName;
    private String frequency; // "DAILY", "WEEKLY", "MONTHLY"
    private long lastLoggedTimestamp;
    private long nextDueTimestamp;
    @androidx.room.ColumnInfo(name = "type", defaultValue = "0")
    private int type; // 0 for Expense, 1 for Income
    private boolean isEnabled;

    public ReminderExpense(String title, double amount, int categoryId, String categoryName, int paymentMethodId, String paymentMethodName, String frequency, long nextDueTimestamp, boolean isEnabled, int type) {
        this.title = title;
        this.amount = amount;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.paymentMethodId = paymentMethodId;
        this.paymentMethodName = paymentMethodName;
        this.frequency = frequency;
        this.nextDueTimestamp = nextDueTimestamp;
        this.isEnabled = isEnabled;
        this.type = type;
        this.lastLoggedTimestamp = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
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

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public long getLastLoggedTimestamp() {
        return lastLoggedTimestamp;
    }

    public void setLastLoggedTimestamp(long lastLoggedTimestamp) {
        this.lastLoggedTimestamp = lastLoggedTimestamp;
    }

    public long getNextDueTimestamp() {
        return nextDueTimestamp;
    }

    public void setNextDueTimestamp(long nextDueTimestamp) {
        this.nextDueTimestamp = nextDueTimestamp;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
