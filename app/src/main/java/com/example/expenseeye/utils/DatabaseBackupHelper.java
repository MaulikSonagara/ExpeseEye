package com.example.expenseeye.utils;

import android.content.Context;
import com.example.expenseeye.database.AppDatabase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class DatabaseBackupHelper {
    public static boolean backupDatabase(Context context, File targetFile) {
        AppDatabase.getDatabase(context).close();
        File dbFile = context.getDatabasePath("expense_eye_database");
        
        if (dbFile.exists()) {
            try {
                copyFile(dbFile, targetFile);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean restoreDatabase(Context context, File sourceFile) {
        AppDatabase.getDatabase(context).close();
        File dbFile = context.getDatabasePath("expense_eye_database");

        try {
            copyFile(sourceFile, dbFile);
            File journalFile = new File(dbFile.getPath() + "-journal");
            if (journalFile.exists()) journalFile.delete();
            File walFile = new File(dbFile.getPath() + "-wal");
            if (walFile.exists()) walFile.delete();
            File shmFile = new File(dbFile.getPath() + "-shm");
            if (shmFile.exists()) shmFile.delete();
            com.example.expenseeye.widget.ExpenseWidgetProvider.updateAllWidgets(context);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void copyFile(File source, File destination) throws IOException {
        try (FileChannel srcChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(destination).getChannel()) {
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        }
    }
}
