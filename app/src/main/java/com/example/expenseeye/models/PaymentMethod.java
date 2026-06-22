package com.example.expenseeye.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "payment_methods")
public class PaymentMethod {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private boolean isDefault;

    public PaymentMethod(String name, boolean isDefault) {
        this.name = name;
        this.isDefault = isDefault;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}
