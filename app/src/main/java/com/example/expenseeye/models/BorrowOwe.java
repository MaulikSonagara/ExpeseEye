package com.example.expenseeye.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "borrow_owe")
public class BorrowOwe {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String personName;
    private double amount;
    private boolean isBorrow; // true if I borrowed from them (I owe them), false if they took from me (They owe me)
    private String description;
    private long timestamp;
    private long dueTimestamp;
    private boolean isSettled;
    private boolean wasAddedAsExpense;

    public BorrowOwe(String personName, double amount, boolean isBorrow, String description, long timestamp, long dueTimestamp, boolean isSettled, boolean wasAddedAsExpense) {
        this.personName = personName;
        this.amount = amount;
        this.isBorrow = isBorrow;
        this.description = description;
        this.timestamp = timestamp;
        this.dueTimestamp = dueTimestamp;
        this.isSettled = isSettled;
        this.wasAddedAsExpense = wasAddedAsExpense;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public boolean isBorrow() {
        return isBorrow;
    }

    public void setBorrow(boolean borrow) {
        isBorrow = borrow;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getDueTimestamp() {
        return dueTimestamp;
    }

    public void setDueTimestamp(long dueTimestamp) {
        this.dueTimestamp = dueTimestamp;
    }

    public boolean isSettled() {
        return isSettled;
    }

    public void setSettled(boolean settled) {
        isSettled = settled;
    }

    public boolean isWasAddedAsExpense() {
        return wasAddedAsExpense;
    }

    public void setWasAddedAsExpense(boolean wasAddedAsExpense) {
        this.wasAddedAsExpense = wasAddedAsExpense;
    }
}
