package com.example.expenseeye.utils;

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
}
