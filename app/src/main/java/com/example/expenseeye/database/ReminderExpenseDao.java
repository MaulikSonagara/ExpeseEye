package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expenseeye.models.ReminderExpense;

import java.util.List;

@Dao
public interface ReminderExpenseDao {
    @Query("SELECT * FROM reminder_expenses WHERE isEnabled = 1")
    LiveData<List<ReminderExpense>> getActiveReminderExpenses();

    @Query("SELECT * FROM reminder_expenses")
    LiveData<List<ReminderExpense>> getAllReminderExpenses();

    @Query("SELECT COUNT(*) FROM reminder_expenses")
    LiveData<Integer> getReminderExpenseCountLive();

    @Query("SELECT * FROM reminder_expenses WHERE isEnabled = 1")
    List<ReminderExpense> getActiveReminderExpensesSync();

    @Query("SELECT * FROM reminder_expenses")
    List<ReminderExpense> getAllReminderExpensesSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReminderExpense reminderExpense);

    @Update
    void update(ReminderExpense reminderExpense);

    @Delete
    void delete(ReminderExpense reminderExpense);
}
