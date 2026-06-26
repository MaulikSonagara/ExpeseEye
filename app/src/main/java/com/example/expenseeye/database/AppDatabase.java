package com.example.expenseeye.database;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.expenseeye.models.Budget;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.CategoryKeyword;
import com.example.expenseeye.models.ChecklistItem;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.PaymentMethod;
import com.example.expenseeye.models.ReminderExpense;
import com.example.expenseeye.models.BorrowOwe;
import com.example.expenseeye.models.BorrowOwePayment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Expense.class, Category.class, PaymentMethod.class, ChecklistItem.class, CategoryKeyword.class, Budget.class, ReminderExpense.class, BorrowOwe.class, BorrowOwePayment.class}, version = AppDatabase.DATABASE_VERSION, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public static final int DATABASE_VERSION = 8;
    public abstract ExpenseDao expenseDao();
    public abstract CategoryDao categoryDao();
    public abstract PaymentMethodDao paymentMethodDao();
    public abstract ChecklistItemDao checklistItemDao();
    public abstract CategoryKeywordsDao categoryKeywordsDao();
    public abstract BudgetDao budgetDao();
    public abstract ReminderExpenseDao reminderExpenseDao();
    public abstract BorrowOweDao borrowOweDao();
    public abstract BorrowOwePaymentDao borrowOwePaymentDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // ... (keep existing migration code)
            database.execSQL("ALTER TABLE categories ADD COLUMN is_enabled INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE categories ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0");
            database.execSQL("CREATE TABLE IF NOT EXISTS category_keywords (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "category_id INTEGER NOT NULL, " +
                    "keyword TEXT, " +
                    "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE)");
            database.execSQL("UPDATE categories SET created_at = " + System.currentTimeMillis());
            database.execSQL("INSERT OR IGNORE INTO categories (name, iconName, color, isDefault, is_enabled, created_at) " +
                    "VALUES ('Bills', 'ic_bills', -26624, 1, 1, 0)");
            
            // Ensure existing Bills category is marked as default system category
            database.execSQL("UPDATE categories SET iconName = 'ic_bills', color = -26624, isDefault = 1 WHERE name = 'Bills'");

            // Migrate all expenses in Water Bill, Electricity, Gas to Bills
            database.execSQL("UPDATE expenses SET category_name = 'Bills' " +
                    "WHERE category_name IN ('Electricity', 'Water Bill', 'Gas')");
            
            // Delete old categories
            database.execSQL("DELETE FROM categories WHERE name IN ('Electricity', 'Water Bill', 'Gas')");

            // 6. Rename Groceries to Food and configure as default system category
            database.execSQL("UPDATE categories SET name = 'Food', iconName = 'ic_food', color = -26368, isDefault = 1 " +
                    "WHERE name = 'Groceries'");
            database.execSQL("UPDATE expenses SET category_name = 'Food' WHERE category_name = 'Groceries'");

            // Ensure Food exists and is configured as default system category
            database.execSQL("INSERT OR IGNORE INTO categories (name, iconName, color, isDefault, is_enabled, created_at) " +
                    "VALUES ('Food', 'ic_food', -26368, 1, 1, 0)");
            database.execSQL("UPDATE categories SET iconName = 'ic_food', color = -26368, isDefault = 1 WHERE name = 'Food'");

            // Ensure Travel is configured as default system category
            database.execSQL("UPDATE categories SET iconName = 'ic_travel', color = -12601712, isDefault = 1 WHERE name = 'Travel'");
            
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

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 1. Create new categories
            database.execSQL("INSERT OR IGNORE INTO categories (name, iconName, color, isDefault, is_enabled, created_at) " +
                    "VALUES ('Groceries', 'ic_groceries', -12601712, 1, 1, " + System.currentTimeMillis() + ")");
            database.execSQL("INSERT OR IGNORE INTO categories (name, iconName, color, isDefault, is_enabled, created_at) " +
                    "VALUES ('Fruit & Veggies', 'ic_food', -7587114, 1, 1, " + System.currentTimeMillis() + ")");

            // 2. Remove "groceries" and "grocery shopping" from existing categories
            database.execSQL("DELETE FROM category_keywords WHERE keyword IN ('groceries', 'grocery shopping')");

            // 3. Add keywords for Groceries
            String[] groceriesKws = {"grocery", "groceries", "kirana", "milk", "bread", "rice", "atta", "flour", "dal", "pulses", "sugar", "salt", "oil", "spices", "masala", "biscuit", "snacks", "chocolate", "tea", "coffee", "maggi", "noodles", "ketchup", "sauce", "paneer", "curd", "butter", "cheese", "eggs", "frozen food", "detergent", "soap", "shampoo", "cleaning", "household", "tissue", "toilet paper", "dishwash", "scrubber", "pocha", "cleaner", "sanitizer", "toothpaste", "toothbrush", "washing powder", "surf", "liquid soap", "dry fruits", "pickle", "jam", "honey", "cereal", "oats"};
            for (String kw : groceriesKws) {
                database.execSQL("INSERT INTO category_keywords (category_id, keyword) " +
                        "SELECT id, '" + kw.replace("'", "''") + "' FROM categories WHERE name = 'Groceries'");
            }

            // 4. Add keywords for Fruit & Veggies
            String[] veggiesKws = {"fruit", "fruits", "vegetable", "vegetables", "sabji", "sabzi", "bhaji", "veg", "mandi", "market", "apple", "banana", "mango", "orange", "grapes", "watermelon", "papaya", "pineapple", "guava", "pomegranate", "chikoo", "kiwi", "coconut", "lemon", "tomato", "potato", "onion", "garlic", "ginger", "carrot", "cabbage", "cauliflower", "broccoli", "spinach", "palak", "methi", "coriander", "capsicum", "cucumber", "brinjal", "eggplant", "peas", "beans", "beetroot", "radish", "chili", "green chili", "pumpkin", "bottle gourd", "ladyfinger", "okra"};
            for (String kw : veggiesKws) {
                database.execSQL("INSERT INTO category_keywords (category_id, keyword) " +
                        "SELECT id, '" + kw.replace("'", "''") + "' FROM categories WHERE name = 'Fruit & Veggies'");
            }
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create budgets table
            database.execSQL("CREATE TABLE IF NOT EXISTS budgets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "categoryName TEXT, " +
                    "month TEXT)");

            // Create recurring_expenses table
            database.execSQL("CREATE TABLE IF NOT EXISTS recurring_expenses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "title TEXT, " +
                    "amount REAL NOT NULL, " +
                    "categoryId INTEGER NOT NULL, " +
                    "categoryName TEXT, " +
                    "paymentMethodId INTEGER NOT NULL, " +
                    "paymentMethodName TEXT, " +
                    "frequency TEXT, " +
                    "lastLoggedTimestamp INTEGER NOT NULL, " +
                    "isEnabled INTEGER NOT NULL)");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE recurring_expenses RENAME TO reminder_expenses");
            database.execSQL("ALTER TABLE reminder_expenses ADD COLUMN nextDueTimestamp INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Check if recurring_expenses table exists
            android.database.Cursor cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='recurring_expenses'");
            boolean hasRecurring = cursor != null && cursor.moveToFirst();
            if (cursor != null) {
                cursor.close();
            }
            if (hasRecurring) {
                database.execSQL("ALTER TABLE recurring_expenses RENAME TO reminder_expenses");
            }

            // Check if nextDueTimestamp column exists in reminder_expenses
            android.database.Cursor colCursor = database.query("PRAGMA table_info(reminder_expenses)");
            boolean hasNextDue = false;
            if (colCursor != null) {
                int nameIndex = colCursor.getColumnIndex("name");
                if (nameIndex != -1) {
                    while (colCursor.moveToNext()) {
                        if ("nextDueTimestamp".equals(colCursor.getString(nameIndex))) {
                            hasNextDue = true;
                            break;
                        }
                    }
                }
                colCursor.close();
            }
            if (!hasNextDue) {
                database.execSQL("ALTER TABLE reminder_expenses ADD COLUMN nextDueTimestamp INTEGER NOT NULL DEFAULT 0");
            }

            // Ensure 'type' column exists in expenses and reminder_expenses
            database.execSQL("ALTER TABLE expenses ADD COLUMN type INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE reminder_expenses ADD COLUMN type INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `borrow_owe` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`personName` TEXT, " +
                    "`amount` REAL NOT NULL, " +
                    "`isBorrow` INTEGER NOT NULL, " +
                    "`description` TEXT, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`dueTimestamp` INTEGER NOT NULL, " +
                    "`isSettled` INTEGER NOT NULL, " +
                    "`wasAddedAsExpense` INTEGER NOT NULL)");
        }
    };

    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `borrow_owe_payments` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`borrowOweId` INTEGER NOT NULL, " +
                    "`amountPaid` REAL NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`note` TEXT, " +
                    "FOREIGN KEY(`borrowOweId`) REFERENCES `borrow_owe`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_borrow_owe_payments_borrowOweId` ON `borrow_owe_payments` (`borrowOweId`)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "expense_eye_database")
                            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        synchronized (AppDatabase.class) {
            if (INSTANCE != null) {
                INSTANCE.close();
                INSTANCE = null;
            }
        }
    }

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                CategoryDao catDao = INSTANCE.categoryDao();
                CategoryKeywordsDao keyDao = INSTANCE.categoryKeywordsDao();

                String[] defaultNames = {"Groceries", "Fruit & Veggies", "Food", "Travel", "Shopping", "Bills", "Health", "Entertainment", "Salary", "Investment", "Education", "Others"};
                String[] defaultIcons = {"ic_groceries", "ic_food", "ic_food", "ic_travel", "ic_shopping", "ic_bills", "ic_medical", "ic_entertainment", "ic_bank", "ic_card", "ic_education", "ic_other"};
                String[] defaultColors = {"#4CAF50", "#8BC34A", "#FF5722", "#4CAF50", "#E91E63", "#FF9800", "#009688", "#9C27B0", "#2196F3", "#3F51B5", "#795548", "#9E9E9E"};

                String[][] defaultKeywords = {
                    {"grocery", "groceries", "kirana", "milk", "bread", "rice", "atta", "flour", "dal", "pulses", "sugar", "salt", "oil", "spices", "masala", "biscuit", "snacks", "chocolate", "tea", "coffee", "maggi", "noodles", "ketchup", "sauce", "paneer", "curd", "butter", "cheese", "eggs", "frozen food", "detergent", "soap", "shampoo", "cleaning", "household", "tissue", "toilet paper", "dishwash", "scrubber", "pocha", "cleaner", "sanitizer", "toothpaste", "toothbrush", "washing powder", "surf", "liquid soap", "dry fruits", "pickle", "jam", "honey", "cereal", "oats"},
                    {"fruit", "fruits", "vegetable", "vegetables", "sabji", "sabzi", "bhaji", "veg", "mandi", "market", "apple", "banana", "mango", "orange", "grapes", "watermelon", "papaya", "pineapple", "guava", "pomegranate", "chikoo", "kiwi", "coconut", "lemon", "tomato", "potato", "onion", "garlic", "ginger", "carrot", "cabbage", "cauliflower", "broccoli", "spinach", "palak", "methi", "coriander", "capsicum", "cucumber", "brinjal", "eggplant", "peas", "beans", "beetroot", "radish", "chili", "green chili", "pumpkin", "bottle gourd", "ladyfinger", "okra"},
                    {"food", "breakfast", "lunch", "dinner", "nasta", "nashta", "samosa", "kachori", "fafda", "jalebi", "chai", "tea", "coffee", "cafe", "pizza", "burger", "sandwich", "dosa", "idli", "vada", "pav bhaji", "vadapav", "bhel", "panipuri", "maggi", "ramen", "swiggy", "zomato", "restaurant", "hotel", "bakery", "snacks", "juice", "cold drink"},
                    {"travel", "trip", "fuel", "petrol", "diesel", "cng", "uber", "ola", "auto", "taxi", "bus", "train", "flight", "booking", "hotel", "hostel", "vacation", "holiday", "toll", "parking", "luggage", "ticket"},
                    {"shopping", "clothes", "shoes", "fashion", "amazon", "flipkart", "myntra", "ajio", "meesho", "accessories", "electronics", "gadget", "mobile", "laptop", "watch", "gift", "furniture", "home decor"},
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
