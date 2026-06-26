package com.example.expenseeye.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.expenseeye.models.BorrowOwePayment;

import java.util.List;

@Dao
public interface BorrowOwePaymentDao {
    @Insert
    long insert(BorrowOwePayment payment);

    @Delete
    void delete(BorrowOwePayment payment);

    @Query("SELECT * FROM borrow_owe_payments WHERE borrowOweId = :borrowOweId ORDER BY timestamp ASC")
    LiveData<List<BorrowOwePayment>> getPaymentsForBorrowOwe(long borrowOweId);

    @Query("SELECT * FROM borrow_owe_payments WHERE borrowOweId = :borrowOweId ORDER BY timestamp ASC")
    List<BorrowOwePayment> getPaymentsForBorrowOweSync(long borrowOweId);

    @Query("SELECT SUM(amountPaid) FROM borrow_owe_payments WHERE borrowOweId = :borrowOweId")
    LiveData<Double> getTotalPaidForBorrowOwe(long borrowOweId);

    @Query("SELECT SUM(amountPaid) FROM borrow_owe_payments WHERE borrowOweId = :borrowOweId")
    Double getTotalPaidForBorrowOweSync(long borrowOweId);

    @Query("SELECT * FROM borrow_owe_payments")
    List<BorrowOwePayment> getAllPaymentsSync();
}
