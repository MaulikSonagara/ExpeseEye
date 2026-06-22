package com.example.expenseeye.database;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.ChecklistItem;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Expense.class, Category.class, PaymentMethod.class, ChecklistItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ExpenseDao expenseDao();
    public abstract CategoryDao categoryDao();
    public abstract PaymentMethodDao paymentMethodDao();
    public abstract ChecklistItemDao checklistItemDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "expense_eye_database")
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                CategoryDao catDao = INSTANCE.categoryDao();
                catDao.insert(new Category("Groceries", "ic_groceries", Color.parseColor("#4CAF50"), true));
                catDao.insert(new Category("Electricity", "ic_electricity", Color.parseColor("#FF9800"), true));
                catDao.insert(new Category("Water Bill", "ic_water", Color.parseColor("#2196F3"), true));
                catDao.insert(new Category("Gas", "ic_gas", Color.parseColor("#FF5722"), true));
                catDao.insert(new Category("Rent", "ic_rent", Color.parseColor("#9C27B0"), true));
                catDao.insert(new Category("Internet", "ic_internet", Color.parseColor("#3F51B5"), true));
                catDao.insert(new Category("Medical", "ic_medical", Color.parseColor("#009688"), true));
                catDao.insert(new Category("Transport", "ic_transport", Color.parseColor("#607D8B"), true));
                catDao.insert(new Category("Shopping", "ic_shopping", Color.parseColor("#E91E63"), true));
                catDao.insert(new Category("Entertainment", "ic_entertainment", Color.parseColor("#00BCD4"), true));
                catDao.insert(new Category("Education", "ic_education", Color.parseColor("#795548"), true));
                catDao.insert(new Category("Other", "ic_other", Color.parseColor("#9E9E9E"), true));

                PaymentMethodDao pmDao = INSTANCE.paymentMethodDao();
                pmDao.insert(new PaymentMethod("Cash", true));
                pmDao.insert(new PaymentMethod("UPI", true));
                pmDao.insert(new PaymentMethod("Credit Card", true));
                pmDao.insert(new PaymentMethod("Debit Card", true));
                pmDao.insert(new PaymentMethod("Bank Transfer", true));
                pmDao.insert(new PaymentMethod("Other", true));
            });
        }
    };
}
