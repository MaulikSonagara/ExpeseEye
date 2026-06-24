package com.example.expenseeye.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String iconName;
    private int color;
    private boolean isDefault;

    @ColumnInfo(name = "is_enabled", defaultValue = "1")
    private boolean isEnabled = true;

    @ColumnInfo(name = "created_at", defaultValue = "0")
    private long createdAt = System.currentTimeMillis();

    public Category(String name, String iconName, int color, boolean isDefault) {
        this.name = name;
        this.iconName = iconName;
        this.color = color;
        this.isDefault = isDefault;
        this.isEnabled = true;
        this.createdAt = System.currentTimeMillis();
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

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
