package com.example.expenseeye.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "debts")
public class Debt {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String personName;
    private double amount;
    private String description;
    private long dueDate;
    private boolean isBorrowed; // true if user borrowed, false if user lent
    private boolean isResolved;
    private long reminderTimestamp;
    private boolean hasReminder;
    private boolean addedAsExpense;

    public Debt(String personName, double amount, String description, long dueDate, boolean isBorrowed, boolean isResolved, long reminderTimestamp, boolean hasReminder, boolean addedAsExpense) {
        this.personName = personName;
        this.amount = amount;
        this.description = description;
        this.dueDate = dueDate;
        this.isBorrowed = isBorrowed;
        this.isResolved = isResolved;
        this.reminderTimestamp = reminderTimestamp;
        this.hasReminder = hasReminder;
        this.addedAsExpense = addedAsExpense;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isBorrowed() {
        return isBorrowed;
    }

    public void setBorrowed(boolean borrowed) {
        isBorrowed = borrowed;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    public long getReminderTimestamp() {
        return reminderTimestamp;
    }

    public void setReminderTimestamp(long reminderTimestamp) {
        this.reminderTimestamp = reminderTimestamp;
    }

    public boolean isHasReminder() {
        return hasReminder;
    }

    public void setHasReminder(boolean hasReminder) {
        this.hasReminder = hasReminder;
    }

    public boolean isAddedAsExpense() {
        return addedAsExpense;
    }

    public void setAddedAsExpense(boolean addedAsExpense) {
        this.addedAsExpense = addedAsExpense;
    }
}
