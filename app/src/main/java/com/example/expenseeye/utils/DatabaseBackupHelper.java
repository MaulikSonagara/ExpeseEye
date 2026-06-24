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
        // Force close database connection to flush memory to disk
        AppDatabase.getDatabase(context).close();
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

    public static boolean validateBackupFile(Context context, Uri sourceUri) {
        File tempFile = new File(context.getCacheDir(), "temp_restore.db");
        if (tempFile.exists()) {
            tempFile.delete();
        }

        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(tempFile)) {
            if (in == null) return false;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            
            // Try opening with SQLiteDatabase to validate
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(tempFile.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                // Check if standard table "expenses" exists
                android.database.Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='expenses'", null);
                boolean hasExpensesTable = cursor != null && cursor.moveToFirst();
                if (cursor != null) cursor.close();
                return hasExpensesTable;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public static boolean restoreDatabase(Context context, Uri sourceUri) {
        // Force close Room database connection
        AppDatabase.getDatabase(context).close();
        File dbFile = context.getDatabasePath("expense_eye_database");

        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(dbFile)) {
            if (in == null) return false;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            
            // Delete secondary database files so they are recreated fresh
            File journalFile = new File(dbFile.getPath() + "-journal");
            if (journalFile.exists()) journalFile.delete();
            File walFile = new File(dbFile.getPath() + "-wal");
            if (walFile.exists()) walFile.delete();
            File shmFile = new File(dbFile.getPath() + "-shm");
            if (shmFile.exists()) shmFile.delete();
            
            com.example.expenseeye.widget.WidgetProvider.updateAllWidgets(context);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
