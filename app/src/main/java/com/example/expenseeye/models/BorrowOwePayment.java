package com.example.expenseeye.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "borrow_owe_payments",
    foreignKeys = @ForeignKey(
        entity = BorrowOwe.class,
        parentColumns = "id",
        childColumns = "borrowOweId",
        onDelete = ForeignKey.CASCADE
    )
)
public class BorrowOwePayment {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long borrowOweId;
    private double amountPaid;
    private long timestamp;
    private String note;

    public BorrowOwePayment(long borrowOweId, double amountPaid, long timestamp, String note) {
        this.borrowOweId = borrowOweId;
        this.amountPaid = amountPaid;
        this.timestamp = timestamp;
        this.note = note;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBorrowOweId() {
        return borrowOweId;
    }

    public void setBorrowOweId(long borrowOweId) {
        this.borrowOweId = borrowOweId;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
