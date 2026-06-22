package com.example.expenseeye.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expenseeye.R;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.utils.DatabaseBackupHelper;
import com.example.expenseeye.utils.ExportHelper;
import com.example.expenseeye.utils.ReminderReceiver;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.materialswitch.MaterialSwitch;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

public class SettingsFragment extends Fragment {

    private AppViewModel viewModel;
    private SharedPreferences sharedPrefs;
    private MaterialSwitch switchDarkMode, switchReminders;
    private EditText etNewCatName;
    private Spinner spinnerColor, spinnerIcon;

    private final String[] colorNames = {"Indigo", "Teal", "Green", "Red", "Orange", "Purple", "Pink", "Grey"};
    private final String[] colorValues = {"#3F51B5", "#008080", "#2E7D32", "#D32F2F", "#EF6C00", "#7B1FA2", "#C2185B", "#616161"};

    private final String[] iconNames = {"Shopping", "Groceries", "Electricity", "Water", "Gas", "Rent", "Internet", "Medical", "Transport", "Entertainment", "Education", "Other"};
    private final String[] iconValues = {"ic_shopping", "ic_groceries", "ic_electricity", "ic_water", "ic_gas", "ic_rent", "ic_internet", "ic_medical", "ic_transport", "ic_entertainment", "ic_education", "ic_other"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        switchReminders = view.findViewById(R.id.switch_daily_reminders);
        etNewCatName = view.findViewById(R.id.et_new_cat_name);
        spinnerColor = view.findViewById(R.id.spinner_new_cat_color);
        spinnerIcon = view.findViewById(R.id.spinner_new_cat_icon);

        Button btnExportPdf = view.findViewById(R.id.btn_export_pdf);
        Button btnExportCsv = view.findViewById(R.id.btn_export_csv);
        Button btnBackup = view.findViewById(R.id.btn_backup);
        Button btnRestore = view.findViewById(R.id.btn_restore);
        Button btnCreateCat = view.findViewById(R.id.btn_create_category);

        sharedPrefs = requireActivity().getSharedPreferences("ExpenseEyePrefs", Context.MODE_PRIVATE);

        // Populate Custom Category spinners
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, colorNames);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerColor.setAdapter(colorAdapter);

        ArrayAdapter<String> iconAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, iconNames);
        iconAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIcon.setAdapter(iconAdapter);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        // Bind Switch states from Shared Prefs
        boolean isDarkMode = sharedPrefs.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDarkMode);

        boolean remindersEnabled = sharedPrefs.getBoolean("reminders", false);
        switchReminders.setChecked(remindersEnabled);

        // Dark Mode switch listener
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPrefs.edit().putBoolean("dark_mode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Reminders switch listener
        switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPrefs.edit().putBoolean("reminders", isChecked).apply();
            setDailyReminder(isChecked);
            if (isChecked) {
                Toast.makeText(getContext(), "Daily reminders set at 8:00 PM", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Reminders disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Export PDF Action
        btnExportPdf.setOnClickListener(v -> exportData("PDF"));

        // Export CSV Action
        btnExportCsv.setOnClickListener(v -> exportData("CSV"));

        // Backup DB Action
        btnBackup.setOnClickListener(v -> backupDatabase());

        // Restore DB Action
        btnRestore.setOnClickListener(v -> restoreDatabase());

        // Create Custom Category Action
        btnCreateCat.setOnClickListener(v -> createCustomCategory());

        return view;
    }

    private void exportData(String format) {
        AsyncTask.execute(() -> {
            List<Expense> expenses = viewModel.getAllExpensesSync();
            if (expenses == null || expenses.isEmpty()) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "No expenses to export", Toast.LENGTH_SHORT).show());
                return;
            }

            String fileName = "ExpenseEye_Report_" + System.currentTimeMillis() + (format.equals("PDF") ? ".pdf" : ".csv");
            File tempFile = new File(requireContext().getCacheDir(), fileName);

            boolean success;
            if (format.equals("PDF")) {
                success = ExportHelper.exportToPDF(expenses, tempFile);
            } else {
                success = ExportHelper.exportToCSV(expenses, tempFile);
            }

            if (success) {
                String mimeType = format.equals("PDF") ? "application/pdf" : "text/csv";
                String savedPath = saveToPublicDownloads(tempFile, fileName, mimeType);

                if (tempFile.exists()) {
                    tempFile.delete();
                }

                if (savedPath != null) {
                    showExportSuccessDialog(savedPath);
                } else {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed to save file to Downloads", Toast.LENGTH_SHORT).show());
                }
            } else {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Export generation failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String saveToPublicDownloads(File tempFile, String fileName, String mimeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = requireContext().getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, mimeType);
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                try (InputStream in = new FileInputStream(tempFile);
                     OutputStream out = resolver.openOutputStream(uri)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    return "Download/" + fileName;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            File destFile = new File(downloadsDir, fileName);
            try (InputStream in = new FileInputStream(tempFile);
                 OutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                return destFile.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void showExportSuccessDialog(String destinationPath) {
        requireActivity().runOnUiThread(() -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Export Successful")
                    .setMessage("Your report has been successfully exported to the public Download folder.\n\nSave Location:\n" + destinationPath)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    private void backupDatabase() {
        AsyncTask.execute(() -> {
            File dir = requireContext().getExternalFilesDir(null);
            File backupFile = new File(dir, "backup_expenseeye.db");
            boolean success = DatabaseBackupHelper.backupDatabase(requireContext(), backupFile);
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Backup created successfully: " + backupFile.getName(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Backup failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void restoreDatabase() {
        AsyncTask.execute(() -> {
            File dir = requireContext().getExternalFilesDir(null);
            File backupFile = new File(dir, "backup_expenseeye.db");
            if (!backupFile.exists()) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "No backup file found to restore!", Toast.LENGTH_SHORT).show());
                return;
            }

            boolean success = DatabaseBackupHelper.restoreDatabase(requireContext(), backupFile);
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Database restored. Please restart the app.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Restore failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void createCustomCategory() {
        String name = etNewCatName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a category name", Toast.LENGTH_SHORT).show();
            return;
        }

        int colorPos = spinnerColor.getSelectedItemPosition();
        int iconPos = spinnerIcon.getSelectedItemPosition();

        int parsedColor = Color.parseColor(colorValues[colorPos]);
        String iconName = iconValues[iconPos];

        Category category = new Category(name, iconName, parsedColor, false);
        viewModel.insertCategory(category);

        etNewCatName.setText("");
        Toast.makeText(getContext(), "Category created: " + name, Toast.LENGTH_SHORT).show();
    }

    private void setDailyReminder(boolean enable) {
        Intent intent = new Intent(getContext(), ReminderReceiver.class);
        intent.putExtra("title", "ExpenseEye Daily Checkin");
        intent.putExtra("message", "Time to log your household expenses or review checklist items!");
        intent.putExtra("id", 101);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(), 101, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        if (enable) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 20); // 8:00 PM
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (alarmManager != null) {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
            }
        } else {
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
        }
    }
}
