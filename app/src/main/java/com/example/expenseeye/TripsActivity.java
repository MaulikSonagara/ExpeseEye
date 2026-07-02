package com.example.expenseeye;

import android.app.DatePickerDialog;
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

import com.example.expenseeye.adapters.TripAdapter;
import com.example.expenseeye.models.Trip;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TripsActivity extends AppCompatActivity {

    private AppViewModel viewModel;
    private TripAdapter adapter;
    private View layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trips);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        layoutEmpty = findViewById(R.id.layout_empty_trips);

        RecyclerView rvTrips = findViewById(R.id.rv_trips);
        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TripAdapter(new TripAdapter.OnTripClickListener() {
            @Override
            public void onTripClick(Trip trip) {
                Intent intent = new Intent(TripsActivity.this, TripDetailsActivity.class);
                intent.putExtra("TRIP_ID", trip.getId());
                startActivity(intent);
            }

            @Override
            public void onTripLongClick(Trip trip) {
                showAddEditTripDialog(trip);
            }
        });
        rvTrips.setAdapter(adapter);

        viewModel.getAllTrips().observe(this, trips -> {
            if (trips == null || trips.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                adapter.submitList(null);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                adapter.submitList(trips);
                
                // Fetch spent for each trip
                for (Trip trip : trips) {
                    viewModel.getExpensesForTrip(trip.getId()).observe(this, expenses -> {
                        double total = 0;
                        if (expenses != null) {
                            for (com.example.expenseeye.models.Expense e : expenses) {
                                total += e.getAmount();
                            }
                        }
                        adapter.setTripSpent(trip.getId(), total);
                    });
                }
            }
        });

        findViewById(R.id.fab_add_trip).setOnClickListener(v -> showAddEditTripDialog(null));
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
        endCal.add(Calendar.DAY_OF_MONTH, 7);

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
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                startCal.set(year, month, dayOfMonth);
                btnStartDate.setText(sdf.format(startCal.getTime()));
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnEndDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
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
            } else {
                Trip newTrip = new Trip(title, etDescription.getText().toString(), startCal.getTimeInMillis(), endCal.getTimeInMillis(), false, budget);
                viewModel.insertTrip(newTrip);
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}
