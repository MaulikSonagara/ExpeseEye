package com.example.expenseeye;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.adapters.ExpenseAdapter;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.models.Trip;
import com.example.expenseeye.theme.ThemePreferenceHelper;
import com.example.expenseeye.utils.ExportHelper;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripDetailsActivity extends AppCompatActivity {

    private AppViewModel viewModel;
    private int tripId;
    private Trip currentTrip;
    private ExpenseAdapter adapter;
    private TextView tvSpent, tvBudget, tvRemaining, tvStatus;
    private LinearProgressIndicator progressTrip;
    private MaterialSwitch switchActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_details);

        tripId = getIntent().getIntExtra("TRIP_ID", -1);
        if (tripId == -1) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        tvSpent = findViewById(R.id.tv_trip_spent);
        tvBudget = findViewById(R.id.tv_trip_budget);
        tvRemaining = findViewById(R.id.tv_trip_remaining);
        tvStatus = findViewById(R.id.tv_trip_status);
        progressTrip = findViewById(R.id.progress_trip);
        switchActive = findViewById(R.id.switch_active_trip);

        RecyclerView rvExpenses = findViewById(R.id.rv_trip_expenses);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpenseAdapter(expense -> {
            // Edit expense logic if needed, or just view
            // For now, let's keep it simple
        });
        rvExpenses.setAdapter(adapter);

        viewModel.getTripById(tripId).observe(this, trip -> {
            if (trip != null) {
                currentTrip = trip;
                getSupportActionBar().setTitle(trip.getTitle());
                updateUi();
            }
        });

        viewModel.getExpensesForTrip(tripId).observe(this, expenses -> {
            adapter.submitExpenseList(expenses);
            updateSpent(expenses);
        });

        switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                viewModel.activateTrip(tripId);
            } else if (currentTrip != null && currentTrip.isActive()) {
                viewModel.deactivateAllTrips();
            }
        });

        findViewById(R.id.btn_export_trip).setOnClickListener(v -> showExportDialog());
        findViewById(R.id.btn_delete_trip).setOnClickListener(v -> {
            if (currentTrip != null) {
                viewModel.deleteTrip(currentTrip);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_trip_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_edit_trip) {
            showAddEditTripDialog(currentTrip);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddEditTripDialog(Trip tripToEdit) {
        Dialog dialog = new Dialog(this, R.style.Theme_ExpenseEye_Dialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_trip, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etTitle = dialogView.findViewById(R.id.et_trip_title);
        EditText etDescription = dialogView.findViewById(R.id.et_trip_description);
        EditText etBudget = dialogView.findViewById(R.id.et_trip_budget);
        Button btnStartDate = dialogView.findViewById(R.id.btn_start_date);
        Button btnEndDate = dialogView.findViewById(R.id.btn_end_date);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        final Calendar startCal = Calendar.getInstance();
        final Calendar endCal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        if (tripToEdit != null) {
            tvTitle.setText("Edit Trip");
            etTitle.setText(tripToEdit.getTitle());
            etDescription.setText(tripToEdit.getDescription());
            etBudget.setText(String.valueOf(tripToEdit.getBudget()));
            startCal.setTimeInMillis(tripToEdit.getStartTimestamp());
            endCal.setTimeInMillis(tripToEdit.getEndTimestamp());
        }

        btnStartDate.setText(sdf.format(startCal.getTime()));
        btnEndDate.setText(sdf.format(endCal.getTime()));

        btnStartDate.setOnClickListener(v -> {
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                startCal.set(year, month, dayOfMonth);
                btnStartDate.setText(sdf.format(startCal.getTime()));
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnEndDate.setOnClickListener(v -> {
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                endCal.set(year, month, dayOfMonth);
                btnEndDate.setText(sdf.format(endCal.getTime()));
            }, endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
                return;
            }

            double budget = 0;
            try {
                budget = Double.parseDouble(etBudget.getText().toString().trim());
            } catch (Exception ignored) {}

            if (tripToEdit != null) {
                tripToEdit.setTitle(title);
                tripToEdit.setDescription(etDescription.getText().toString());
                tripToEdit.setStartTimestamp(startCal.getTimeInMillis());
                tripToEdit.setEndTimestamp(endCal.getTimeInMillis());
                tripToEdit.setBudget(budget);
                viewModel.updateTrip(tripToEdit);
                Toast.makeText(this, "Trip updated successfully", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateUi() {
        if (currentTrip == null) return;

        ThemePreferenceHelper ph = new ThemePreferenceHelper(this);
        String currency = ph.getCurrencySymbol();

        tvBudget.setText(String.format(Locale.getDefault(), "%s%.2f", currency, currentTrip.getBudget()));
        switchActive.setChecked(currentTrip.isActive());
        tvStatus.setText(currentTrip.isActive() ? "Active (Linking new expenses)" : "Inactive");
    }

    private void updateSpent(List<Expense> expenses) {
        double total = 0;
        if (expenses != null) {
            for (Expense e : expenses) {
                total += e.getAmount();
            }
        }

        ThemePreferenceHelper ph = new ThemePreferenceHelper(this);
        String currency = ph.getCurrencySymbol();
        tvSpent.setText(String.format(Locale.getDefault(), "%s%.2f", currency, total));

        if (currentTrip != null) {
            double budget = currentTrip.getBudget();
            if (budget > 0) {
                double remaining = budget - total;
                tvRemaining.setText(String.format(Locale.getDefault(), "%s%.2f remaining", currency, remaining));
                int progress = (int) ((total / budget) * 100);
                progressTrip.setProgress(Math.min(progress, 100));
            } else {
                tvRemaining.setText("No budget set");
                progressTrip.setProgress(0);
            }
        }
    }

    private void showExportDialog() {
        Dialog dialog = new Dialog(this, R.style.Theme_ExpenseEye_Dialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_export_trip, null);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btn_export_pdf).setOnClickListener(v -> {
            exportTrip("PDF");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_export_csv).setOnClickListener(v -> {
            exportTrip("CSV");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void exportTrip(String format) {
        if (currentTrip == null) return;
        
        viewModel.getExpensesForTrip(tripId).observe(this, expenses -> {
            if (expenses == null || expenses.isEmpty()) {
                Toast.makeText(this, "No expenses to export", Toast.LENGTH_SHORT).show();
                return;
            }

            File exportFile;
            boolean success;
            String fileName = "Trip_" + currentTrip.getTitle().replaceAll("\\s+", "_") + "_" + System.currentTimeMillis();
            
            try {
                if (format.equals("PDF")) {
                    exportFile = new File(getExternalFilesDir(null), fileName + ".pdf");
                    success = ExportHelper.exportToPDF(expenses, exportFile, currentTrip.getStartTimestamp(), currentTrip.getEndTimestamp(), currentTrip.getTitle());
                } else {
                    exportFile = new File(getExternalFilesDir(null), fileName + ".csv");
                    success = ExportHelper.exportToCSV(expenses, exportFile);
                }

                if (success) {
                    Toast.makeText(this, format + " Exported to " + exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Export error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
