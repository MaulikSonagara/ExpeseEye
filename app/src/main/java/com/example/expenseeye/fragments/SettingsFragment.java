package com.example.expenseeye.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.adapters.ThemeSelectionAdapter;
import com.example.expenseeye.database.AppDatabase;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.theme.ThemePreferenceHelper;
import com.example.expenseeye.utils.DatabaseBackupHelper;
import com.example.expenseeye.utils.ExportHelper;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private ThemePreferenceHelper themeHelper;
    private AppViewModel viewModel;

    // Flag to determine which export requires permission upon callback
    private boolean pendingPdfExport = false;
    private boolean pendingCsvExport = false;

    // Activity result launcher for Storage Permission (Android 9 and below)
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    if (pendingPdfExport) {
                        pendingPdfExport = false;
                        exportPdfFlow();
                    } else if (pendingCsvExport) {
                        pendingCsvExport = false;
                        exportCsvFlow();
                    }
                } else {
                    pendingPdfExport = false;
                    pendingCsvExport = false;
                    Toast.makeText(getContext(), "Storage permission is required to save files on older devices.", Toast.LENGTH_LONG).show();
                }
            });

    // SAF backup file destination selector
    private final ActivityResultLauncher<Intent> backupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performBackup(uri);
                    }
                }
            });

    // SAF restore file picker
    private final ActivityResultLauncher<Intent> restoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performRestore(uri);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        themeHelper = new ThemePreferenceHelper(requireContext());
        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        setupThemeSelection(view);
        setupModeSwitch(view);
        setupActionButtons(view);
    }

    private void setupThemeSelection(View view) {
        RecyclerView rvThemes = view.findViewById(R.id.rv_themes);
        List<String> themes = Arrays.asList(
                ThemePreferenceHelper.THEME_MIDNIGHT,
                ThemePreferenceHelper.THEME_FOREST,
                ThemePreferenceHelper.THEME_SAND,
                ThemePreferenceHelper.THEME_OCEAN
        );

        ThemeSelectionAdapter adapter = new ThemeSelectionAdapter(themes, themeHelper.getTheme(), themeName -> {
            themeHelper.setTheme(themeName);
            requireActivity().recreate();
        });

        rvThemes.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvThemes.setAdapter(adapter);
    }

    private void setupModeSwitch(View view) {
        MaterialSwitch switchDark = view.findViewById(R.id.switch_dark_mode);
        switchDark.setChecked(themeHelper.isDarkMode());

        switchDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeHelper.setMode(isChecked ? ThemePreferenceHelper.MODE_DARK : ThemePreferenceHelper.MODE_LIGHT);
            requireActivity().recreate();
        });
    }

    private void setupActionButtons(View view) {
        view.findViewById(R.id.btn_export_pdf).setOnClickListener(v -> exportPdfFlow());
        view.findViewById(R.id.btn_export_csv).setOnClickListener(v -> exportCsvFlow());
        
        view.findViewById(R.id.btn_backup).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/x-sqlite3");
            String fileName = "expense_eye_backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".db";
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            backupLauncher.launch(intent);
        });

        view.findViewById(R.id.btn_restore).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            restoreLauncher.launch(intent);
        });

        view.findViewById(R.id.btn_manage_categories).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.example.expenseeye.CategoryManagementActivity.class);
            startActivity(intent);
        });
    }

    // --- PDF Export Flow ---
    private void exportPdfFlow() {
        if (!checkStoragePermission()) {
            pendingPdfExport = true;
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Expense> expenses = viewModel.getAllExpensesSync();
            if (expenses == null || expenses.isEmpty()) {
                showToastOnMainThread("No expenses to export.");
                return;
            }

            try {
                File tempFile = new File(requireContext().getCacheDir(), "expense_report_" + System.currentTimeMillis() + ".pdf");
                boolean exportSuccess = ExportHelper.exportToPDF(expenses, tempFile);
                if (exportSuccess) {
                    String fileName = "ExpenseEye_Report_" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()) + ".pdf";
                    boolean saveSuccess = saveFileToDownloads(tempFile, fileName, "application/pdf");
                    if (saveSuccess) {
                        showToastOnMainThread("PDF Saved successfully in Downloads.");
                        shareFile(tempFile, "application/pdf", "Share PDF Report");
                    } else {
                        showToastOnMainThread("Failed to save PDF to storage.");
                    }
                } else {
                    showToastOnMainThread("Failed to generate PDF Report.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToastOnMainThread("Error occurred during PDF generation.");
            }
        });
    }

    // --- CSV Export Flow ---
    private void exportCsvFlow() {
        if (!checkStoragePermission()) {
            pendingCsvExport = true;
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Expense> expenses = viewModel.getAllExpensesSync();
            if (expenses == null || expenses.isEmpty()) {
                showToastOnMainThread("No expenses to export.");
                return;
            }

            try {
                File tempFile = new File(requireContext().getCacheDir(), "expense_sheet_" + System.currentTimeMillis() + ".csv");
                boolean exportSuccess = ExportHelper.exportToCSV(expenses, tempFile);
                if (exportSuccess) {
                    String fileName = "ExpenseEye_Sheet_" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()) + ".csv";
                    boolean saveSuccess = saveFileToDownloads(tempFile, fileName, "text/csv");
                    if (saveSuccess) {
                        showToastOnMainThread("CSV Saved successfully in Downloads.");
                        shareFile(tempFile, "text/csv", "Share CSV Sheet");
                    } else {
                        showToastOnMainThread("Failed to save CSV to storage.");
                    }
                } else {
                    showToastOnMainThread("Failed to generate CSV Sheet.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToastOnMainThread("Error occurred during CSV generation.");
            }
        });
    }

    // --- Database Backup ---
    private void performBackup(Uri targetUri) {
        new Thread(() -> {
            boolean success = DatabaseBackupHelper.backupDatabase(requireContext(), targetUri);
            if (success) {
                showToastOnMainThread("Database backup created successfully!");
            } else {
                showToastOnMainThread("Database backup failed.");
            }
        }).start();
    }

    // --- Database Restore ---
    private void performRestore(Uri sourceUri) {
        new Thread(() -> {
            // 1. Validate file before restoring
            boolean isValid = DatabaseBackupHelper.validateBackupFile(requireContext(), sourceUri);
            if (!isValid) {
                showToastOnMainThread("Invalid backup file. Restore aborted to prevent crashes.");
                return;
            }

            // 2. Perform safe restore
            boolean success = DatabaseBackupHelper.restoreDatabase(requireContext(), sourceUri);
            if (success) {
                showToastOnMainThread("Database restored successfully! Restarting app...");
                
                // Restart app on main thread to apply changes cleanly
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(getActivity().getPackageName());
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                        getActivity().finishAffinity();
                        System.exit(0);
                    });
                }
            } else {
                showToastOnMainThread("Database restore failed.");
            }
        }).start();
    }

    // --- Helpers ---
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            // Android 10+ (Q) uses Scoped Storage, no storage permissions needed for Downloads directory
            return true;
        }
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean saveFileToDownloads(File sourceFile, String displayName, String mimeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
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
        } else {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            File targetFile = new File(downloadsDir, displayName);
            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = new FileOutputStream(targetFile)) {
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

    private void shareFile(File file, String mimeType, String chooserTitle) {
        try {
            Uri fileUri = FileProvider.getUriForFile(requireContext(), "com.example.expenseeye.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, chooserTitle));
        } catch (Exception e) {
            e.printStackTrace();
            showToastOnMainThread("Failed to share file.");
        }
    }

    private void showToastOnMainThread(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }
}
