package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expenseeye.models.PaymentMethod;

import java.util.List;

@Dao
public interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY name ASC")
    LiveData<List<PaymentMethod>> getAllPaymentMethods();

    @Query("SELECT * FROM payment_methods ORDER BY name ASC")
    List<PaymentMethod> getAllPaymentMethodsSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PaymentMethod paymentMethod);

    @Update
    void update(PaymentMethod paymentMethod);

    @Delete
    void delete(PaymentMethod paymentMethod);

    @Query("SELECT * FROM payment_methods WHERE name = :name LIMIT 1")
    PaymentMethod getByName(String name);

    @Query("SELECT COUNT(*) FROM payment_methods")
    int getCount();
}
