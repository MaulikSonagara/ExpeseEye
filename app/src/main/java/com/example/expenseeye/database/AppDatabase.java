package com.example.expenseeye.database;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.CategoryKeyword;
import com.example.expenseeye.models.ChecklistItem;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Expense.class, Category.class, PaymentMethod.class, ChecklistItem.class, CategoryKeyword.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ExpenseDao expenseDao();
    public abstract CategoryDao categoryDao();
    public abstract PaymentMethodDao paymentMethodDao();
    public abstract ChecklistItemDao checklistItemDao();
    public abstract CategoryKeywordsDao categoryKeywordsDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 1. Add is_enabled column to categories
            database.execSQL("ALTER TABLE categories ADD COLUMN is_enabled INTEGER NOT NULL DEFAULT 1");
            
            // 2. Add created_at column to categories
            database.execSQL("ALTER TABLE categories ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0");
            
            // 3. Create the category_keywords table
            database.execSQL("CREATE TABLE IF NOT EXISTS category_keywords (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "category_id INTEGER NOT NULL, " +
                    "keyword TEXT, " +
                    "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE)");
                    
            // 4. Update the created_at to current timestamp for existing rows
            database.execSQL("UPDATE categories SET created_at = " + System.currentTimeMillis());
            
            // 5. Merge legacy Water Bill, Electricity, and Gas into Bills category
            // First, create the Bills category if it doesn't exist (configured as default system category)
            database.execSQL("INSERT OR IGNORE INTO categories (name, icon_name, color, isDefault, is_enabled, created_at) " +
                    "VALUES ('Bills', 'ic_bills', -26624, 1, 1, 0)");
            
            // Ensure existing Bills category is marked as default system category
            database.execSQL("UPDATE categories SET icon_name = 'ic_bills', color = -26624, isDefault = 1 WHERE name = 'Bills'");

            // Migrate all expenses in Water Bill, Electricity, Gas to Bills
            database.execSQL("UPDATE expenses SET category_name = 'Bills' " +
                    "WHERE category_name IN ('Electricity', 'Water Bill', 'Gas')");
            
            // Delete old categories
            database.execSQL("DELETE FROM categories WHERE name IN ('Electricity', 'Water Bill', 'Gas')");

            // 6. Rename Groceries to Food and configure as default system category
            database.execSQL("UPDATE categories SET name = 'Food', icon_name = 'ic_food', color = -26368, isDefault = 1 " +
                    "WHERE name = 'Groceries'");
            database.execSQL("UPDATE expenses SET category_name = 'Food' WHERE category_name = 'Groceries'");

            // Ensure Food exists and is configured as default system category
            database.execSQL("INSERT OR IGNORE INTO categories (name, icon_name, color, isDefault, is_enabled, created_at) " +
                    "VALUES ('Food', 'ic_food', -26368, 1, 1, 0)");
            database.execSQL("UPDATE categories SET icon_name = 'ic_food', color = -26368, isDefault = 1 WHERE name = 'Food'");

            // Ensure Travel is configured as default system category
            database.execSQL("UPDATE categories SET icon_name = 'ic_travel', color = -12601712, isDefault = 1 WHERE name = 'Travel'");
            
            // 7. Prepopulate category keywords for standard categories if they exist
            String[] defaultNames = {"Food", "Travel", "Shopping", "Bills", "Health", "Entertainment", "Salary", "Investment", "Education", "Others"};
            String[][] defaultKeywords = {
                {"food", "breakfast", "lunch", "dinner", "nasta", "nashta", "samosa", "kachori", "fafda", "jalebi", "chai", "tea", "coffee", "cafe", "pizza", "burger", "sandwich", "dosa", "idli", "vada", "pav bhaji", "vadapav", "bhel", "panipuri", "maggi", "ramen", "groceries", "swiggy", "zomato", "restaurant", "hotel", "bakery", "snacks", "juice", "cold drink"},
                {"travel", "trip", "fuel", "petrol", "diesel", "cng", "uber", "ola", "auto", "taxi", "bus", "train", "flight", "booking", "hotel", "hostel", "vacation", "holiday", "toll", "parking", "luggage", "ticket"},
                {"shopping", "clothes", "shoes", "fashion", "amazon", "flipkart", "myntra", "ajio", "meesho", "accessories", "electronics", "gadget", "mobile", "laptop", "watch", "gift", "furniture", "home decor", "grocery shopping"},
                {"electricity", "light bill", "power bill", "gas bill", "water bill", "internet", "wifi", "broadband", "recharge", "mobile bill", "postpaid", "prepaid", "dth", "cable", "rent", "emi", "subscription", "netflix", "hotstar", "prime", "spotify", "maintenance", "society bill"},
                {"doctor", "hospital", "clinic", "medicine", "pharmacy", "medical", "tablet", "injection", "surgery", "test", "blood test", "checkup", "dentist", "eye care", "health insurance", "gym", "protein", "supplement"},
                {"movie", "cinema", "game", "gaming", "steam", "psn", "xbox", "netflix", "hotstar", "amazon prime", "spotify", "party", "club", "concert", "event", "picnic", "amusement park", "cricket match"},
                {"salary", "income", "paycheck", "stipend", "freelance", "bonus", "incentive", "payout", "profit", "wage", "monthly income", "commission"},
                {"investment", "stock", "share", "mutual fund", "sip", "fd", "rd", "crypto", "bitcoin", "gold", "silver", "trading", "demat", "zerodha", "groww", "upstox", "angel one", "bond", "equity", "dividend"},
                {"school", "college", "fees", "tuition", "coaching", "books", "notebook", "stationery", "exam fee", "course", "udemy", "coursera", "class", "workshop", "seminar", "project", "training", "hostel fee"},
                {"miscellaneous", "other", "random", "unknown", "general", "extra", "uncategorized"}
            };

            for (int i = 0; i < defaultNames.length; i++) {
                for (String kw : defaultKeywords[i]) {
                    database.execSQL("INSERT INTO category_keywords (category_id, keyword) " +
                            "SELECT id, '" + kw.replace("'", "''") + "' FROM categories WHERE name = '" + defaultNames[i] + "'");
                }
            }
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "expense_eye_database")
                            .addMigrations(MIGRATION_1_2)
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
                CategoryKeywordsDao keyDao = INSTANCE.categoryKeywordsDao();

                String[] defaultNames = {"Food", "Travel", "Shopping", "Bills", "Health", "Entertainment", "Salary", "Investment", "Education", "Others"};
                String[] defaultIcons = {"ic_food", "ic_travel", "ic_shopping", "ic_bills", "ic_medical", "ic_entertainment", "ic_bank", "ic_card", "ic_education", "ic_other"};
                String[] defaultColors = {"#FF5722", "#4CAF50", "#E91E63", "#FF9800", "#009688", "#9C27B0", "#2196F3", "#3F51B5", "#795548", "#9E9E9E"};

                String[][] defaultKeywords = {
                    {"food", "breakfast", "lunch", "dinner", "nasta", "nashta", "samosa", "kachori", "fafda", "jalebi", "chai", "tea", "coffee", "cafe", "pizza", "burger", "sandwich", "dosa", "idli", "vada", "pav bhaji", "vadapav", "bhel", "panipuri", "maggi", "ramen", "groceries", "swiggy", "zomato", "restaurant", "hotel", "bakery", "snacks", "juice", "cold drink"},
                    {"travel", "trip", "fuel", "petrol", "diesel", "cng", "uber", "ola", "auto", "taxi", "bus", "train", "flight", "booking", "hotel", "hostel", "vacation", "holiday", "toll", "parking", "luggage", "ticket"},
                    {"shopping", "clothes", "shoes", "fashion", "amazon", "flipkart", "myntra", "ajio", "meesho", "accessories", "electronics", "gadget", "mobile", "laptop", "watch", "gift", "furniture", "home decor", "grocery shopping"},
                    {"electricity", "light bill", "power bill", "gas bill", "water bill", "internet", "wifi", "broadband", "recharge", "mobile bill", "postpaid", "prepaid", "dth", "cable", "rent", "emi", "subscription", "netflix", "hotstar", "prime", "spotify", "maintenance", "society bill"},
                    {"doctor", "hospital", "clinic", "medicine", "pharmacy", "medical", "tablet", "injection", "surgery", "test", "blood test", "checkup", "dentist", "eye care", "health insurance", "gym", "protein", "supplement"},
                    {"movie", "cinema", "game", "gaming", "steam", "psn", "xbox", "netflix", "hotstar", "amazon prime", "spotify", "party", "club", "concert", "event", "picnic", "amusement park", "cricket match"},
                    {"salary", "income", "paycheck", "stipend", "freelance", "bonus", "incentive", "payout", "profit", "wage", "monthly income", "commission"},
                    {"investment", "stock", "share", "mutual fund", "sip", "fd", "rd", "crypto", "bitcoin", "gold", "silver", "trading", "demat", "zerodha", "groww", "upstox", "angel one", "bond", "equity", "dividend"},
                    {"school", "college", "fees", "tuition", "coaching", "books", "notebook", "stationery", "exam fee", "course", "udemy", "coursera", "class", "workshop", "seminar", "project", "training", "hostel fee"},
                    {"miscellaneous", "other", "random", "unknown", "general", "extra", "uncategorized"}
                };

                for (int i = 0; i < defaultNames.length; i++) {
                    long catId = catDao.insert(new Category(defaultNames[i], defaultIcons[i], Color.parseColor(defaultColors[i]), true));
                    for (String kw : defaultKeywords[i]) {
                        keyDao.insert(new CategoryKeyword((int) catId, kw));
                    }
                }

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
