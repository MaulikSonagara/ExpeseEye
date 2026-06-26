package com.example.expenseeye.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.example.expenseeye.database.AppDatabase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseBackupHelper {

    public static boolean backupDatabase(Context context, Uri targetUri) {
        try {
            AppDatabase db = AppDatabase.getDatabase(context);
            // Force a full checkpoint to merge WAL content into the main DB file
            // This ensures NO data is left behind in -wal or -shm files.
            try (android.database.Cursor cursor = db.getOpenHelper().getWritableDatabase().query("PRAGMA wal_checkpoint(FULL)", null)) {
                if (cursor != null) {
                    cursor.moveToFirst();
                }
            }
            // Close the database to release file locks
            AppDatabase.destroyInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File dbFile = context.getDatabasePath("expense_eye_database");
        if (dbFile.exists()) {
            try (InputStream in = new FileInputStream(dbFile);
                 OutputStream out = context.getContentResolver().openOutputStream(targetUri)) {
                if (out == null) return false;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;
        public final int version;

        public ValidationResult(boolean isValid, String errorMessage, int version) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.version = version;
        }

        public static ValidationResult valid(int version) {
            return new ValidationResult(true, null, version);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message, -1);
        }
    }

    public static ValidationResult validateBackupFile(Context context, Uri sourceUri) {
        File tempFile = new File(context.getCacheDir(), "temp_restore.db");
        if (tempFile.exists()) {
            tempFile.delete();
        }

        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(tempFile)) {
            if (in == null) return ValidationResult.invalid("Unable to open backup file");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            
            // Try opening with SQLiteDatabase to validate
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(tempFile.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                // Check for essential tables
                boolean hasExpenses = false;
                boolean hasCategories = false;
                
                try (android.database.Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('expenses', 'categories')", null)) {
                    while (cursor != null && cursor.moveToNext()) {
                        String tableName = cursor.getString(0);
                        if ("expenses".equals(tableName)) hasExpenses = true;
                        if ("categories".equals(tableName)) hasCategories = true;
                    }
                }

                if (!hasExpenses || !hasCategories) {
                    return ValidationResult.invalid("Required data tables (expenses/categories) are missing in backup");
                }

                int version = db.getVersion();
                return ValidationResult.valid(version);
            } catch (Exception e) {
                return ValidationResult.invalid("File is not a valid SQLite database or is corrupted");
            }
        } catch (IOException e) {
            return ValidationResult.invalid("IO Error: " + e.getMessage());
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public static boolean restoreDatabase(Context context, Uri sourceUri) {
        // Force close Room database connection and reset the instance
        AppDatabase.destroyInstance();
        File dbFile = context.getDatabasePath("expense_eye_database");

        // Delete existing database file and secondary SQLite helper files BEFORE copying
        if (dbFile.exists()) {
            dbFile.delete();
        }
        File journalFile = new File(dbFile.getPath() + "-journal");
        if (journalFile.exists()) journalFile.delete();
        File walFile = new File(dbFile.getPath() + "-wal");
        if (walFile.exists()) walFile.delete();
        File shmFile = new File(dbFile.getPath() + "-shm");
        if (shmFile.exists()) shmFile.delete();

        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(dbFile)) {
            if (in == null) return false;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            
            com.example.expenseeye.widget.WidgetProvider.updateAllWidgets(context);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean mergeDatabase(Context context, Uri sourceUri) {
        // 1. Copy backup to temp file for reading
        File tempFile = new File(context.getCacheDir(), "merge_source.db");
        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(tempFile)) {
            if (in == null) return false;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // 2. Perform merge using raw SQLite on the background thread
        try (SQLiteDatabase sourceDb = SQLiteDatabase.openDatabase(tempFile.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
            AppDatabase targetRoomDb = AppDatabase.getDatabase(context);
            // We use targetRoomDb.getOpenHelper().getWritableDatabase() to get a SupportSQLiteDatabase
            androidx.sqlite.db.SupportSQLiteDatabase targetDb = targetRoomDb.getOpenHelper().getWritableDatabase();

            targetDb.beginTransaction();
            try {
                // Merge Categories (Insert or Ignore)
                try (android.database.Cursor cursor = sourceDb.query("categories", null, null, null, null, null, null)) {
                    while (cursor.moveToNext()) {
                        ContentValues values = new ContentValues();
                        values.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")));
                        values.put("iconName", cursor.getString(cursor.getColumnIndexOrThrow("iconName")));
                        values.put("color", cursor.getInt(cursor.getColumnIndexOrThrow("color")));
                        values.put("isDefault", cursor.getInt(cursor.getColumnIndexOrThrow("isDefault")));
                        values.put("is_enabled", cursor.getInt(cursor.getColumnIndexOrThrow("is_enabled")));
                        values.put("created_at", cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
                        targetDb.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values);
                    }
                }

                // Merge Payment Methods
                try (android.database.Cursor cursor = sourceDb.query("payment_methods", null, null, null, null, null, null)) {
                    while (cursor.moveToNext()) {
                        ContentValues values = new ContentValues();
                        values.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")));
                        values.put("isEnabled", cursor.getInt(cursor.getColumnIndexOrThrow("isEnabled")));
                        targetDb.insert("payment_methods", SQLiteDatabase.CONFLICT_IGNORE, values);
                    }
                }

                // Merge Expenses
                try (android.database.Cursor cursor = sourceDb.query("expenses", null, null, null, null, null, null)) {
                    while (cursor.moveToNext()) {
                        ContentValues values = new ContentValues();
                        values.put("title", cursor.getString(cursor.getColumnIndexOrThrow("title")));
                        values.put("description", cursor.getString(cursor.getColumnIndexOrThrow("description")));
                        values.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow("amount")));
                        values.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
                        values.put("categoryId", cursor.getInt(cursor.getColumnIndexOrThrow("categoryId")));
                        values.put("categoryName", cursor.getString(cursor.getColumnIndexOrThrow("categoryName")));
                        values.put("paymentMethodId", cursor.getInt(cursor.getColumnIndexOrThrow("paymentMethodId")));
                        values.put("paymentMethodName", cursor.getString(cursor.getColumnIndexOrThrow("paymentMethodName")));
                        // Handle 'type' if it exists in source
                        int typeIndex = cursor.getColumnIndex("type");
                        if (typeIndex != -1) {
                            values.put("type", cursor.getInt(typeIndex));
                        }
                        targetDb.insert("expenses", SQLiteDatabase.CONFLICT_IGNORE, values);
                    }
                }

                // Merge Budgets
                try (android.database.Cursor cursor = sourceDb.query("budgets", null, null, null, null, null, null)) {
                    while (cursor.moveToNext()) {
                        ContentValues values = new ContentValues();
                        values.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow("amount")));
                        values.put("categoryName", cursor.getString(cursor.getColumnIndexOrThrow("categoryName")));
                        values.put("month", cursor.getString(cursor.getColumnIndexOrThrow("month")));
                        targetDb.insert("budgets", SQLiteDatabase.CONFLICT_IGNORE, values);
                    }
                }

                // Merge Reminder Expenses
                try (android.database.Cursor cursor = sourceDb.query("reminder_expenses", null, null, null, null, null, null)) {
                    while (cursor.moveToNext()) {
                        ContentValues values = new ContentValues();
                        values.put("title", cursor.getString(cursor.getColumnIndexOrThrow("title")));
                        values.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow("amount")));
                        values.put("categoryId", cursor.getInt(cursor.getColumnIndexOrThrow("categoryId")));
                        values.put("categoryName", cursor.getString(cursor.getColumnIndexOrThrow("categoryName")));
                        values.put("paymentMethodId", cursor.getInt(cursor.getColumnIndexOrThrow("paymentMethodId")));
                        values.put("paymentMethodName", cursor.getString(cursor.getColumnIndexOrThrow("paymentMethodName")));
                        values.put("frequency", cursor.getString(cursor.getColumnIndexOrThrow("frequency")));
                        values.put("lastLoggedTimestamp", cursor.getLong(cursor.getColumnIndexOrThrow("lastLoggedTimestamp")));
                        values.put("nextDueTimestamp", cursor.getLong(cursor.getColumnIndexOrThrow("nextDueTimestamp")));
                        values.put("isEnabled", cursor.getInt(cursor.getColumnIndexOrThrow("isEnabled")));
                        int typeIndex = cursor.getColumnIndex("type");
                        if (typeIndex != -1) {
                            values.put("type", cursor.getInt(typeIndex));
                        }
                        targetDb.insert("reminder_expenses", SQLiteDatabase.CONFLICT_IGNORE, values);
                    }
                }

                // Merge Borrow & Owe
                java.util.Map<Long, Long> borrowOweIdMap = new java.util.HashMap<>();
                boolean hasBorrowOwe = false;
                try (android.database.Cursor checkCursor = sourceDb.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = 'borrow_owe'", null)) {
                    if (checkCursor != null && checkCursor.getCount() > 0) {
                        hasBorrowOwe = true;
                    }
                }
                if (hasBorrowOwe) {
                    try (android.database.Cursor cursor = sourceDb.query("borrow_owe", null, null, null, null, null, null)) {
                        while (cursor.moveToNext()) {
                            long oldId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                            ContentValues values = new ContentValues();
                            values.put("personName", cursor.getString(cursor.getColumnIndexOrThrow("personName")));
                            values.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow("amount")));
                            values.put("isBorrow", cursor.getInt(cursor.getColumnIndexOrThrow("isBorrow")));
                            values.put("description", cursor.getString(cursor.getColumnIndexOrThrow("description")));
                            values.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
                            values.put("dueTimestamp", cursor.getLong(cursor.getColumnIndexOrThrow("dueTimestamp")));
                            values.put("isSettled", cursor.getInt(cursor.getColumnIndexOrThrow("isSettled")));
                            values.put("wasAddedAsExpense", cursor.getInt(cursor.getColumnIndexOrThrow("wasAddedAsExpense")));
                            long newId = targetDb.insert("borrow_owe", SQLiteDatabase.CONFLICT_IGNORE, values);
                            if (newId != -1) {
                                borrowOweIdMap.put(oldId, newId);
                            }
                        }
                    }
                }

                // Merge Borrow & Owe Payments
                boolean hasBorrowOwePayments = false;
                try (android.database.Cursor checkCursor = sourceDb.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = 'borrow_owe_payments'", null)) {
                    if (checkCursor != null && checkCursor.getCount() > 0) {
                        hasBorrowOwePayments = true;
                    }
                }
                if (hasBorrowOwePayments) {
                    try (android.database.Cursor cursor = sourceDb.query("borrow_owe_payments", null, null, null, null, null, null)) {
                        while (cursor.moveToNext()) {
                            long oldBorrowOweId = cursor.getLong(cursor.getColumnIndexOrThrow("borrowOweId"));
                            if (borrowOweIdMap.containsKey(oldBorrowOweId)) {
                                long newBorrowOweId = borrowOweIdMap.get(oldBorrowOweId);
                                ContentValues values = new ContentValues();
                                values.put("borrowOweId", newBorrowOweId);
                                values.put("amountPaid", cursor.getDouble(cursor.getColumnIndexOrThrow("amountPaid")));
                                values.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
                                values.put("note", cursor.getString(cursor.getColumnIndexOrThrow("note")));
                                targetDb.insert("borrow_owe_payments", SQLiteDatabase.CONFLICT_IGNORE, values);
                            }
                        }
                    }
                }
                
                targetDb.setTransactionSuccessful();
            } finally {
                targetDb.endTransaction();
            }

            com.example.expenseeye.widget.WidgetProvider.updateAllWidgets(context);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }
}
