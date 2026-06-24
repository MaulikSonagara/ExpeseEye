package com.example.expenseeye.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expenseeye.R;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.Expense;
import com.example.expenseeye.viewmodel.AppViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class ReportsFragment extends Fragment {

    private AppViewModel viewModel;
    private com.google.android.material.chip.ChipGroup cgFilterRange;
    private TextView tvDailyAverage, tvTopCategory;
    private PieChart pieChart;
    private LineChart lineChart;
    private BarChart barChart;

    private List<Expense> allExpensesList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        cgFilterRange = view.findViewById(R.id.cg_filter_range);
        tvDailyAverage = view.findViewById(R.id.tv_daily_average);
        tvTopCategory = view.findViewById(R.id.tv_top_category);
        pieChart = view.findViewById(R.id.pie_chart_categories);
        lineChart = view.findViewById(R.id.line_chart_trend);
        barChart = view.findViewById(R.id.bar_chart_payments);

        setupCharts();

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        // Observe all expenses to compile statistics dynamically
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                allExpensesList = expenses;
                updateReportData();
            }
        });

        // Trigger updates when checked chip changes
        cgFilterRange.setOnCheckedStateChangeListener((group, checkedIds) -> {
            updateReportData();
        });

        return view;
    }

    private int getThemeTextColor() {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)) {
            return typedValue.data;
        }
        return Color.BLACK;
    }

    private void setupCharts() {
        int textColor = getThemeTextColor();

        // Pie Chart Configuration
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.setEntryLabelColor(textColor);
        pieChart.getLegend().setTextColor(textColor);

        // Line Chart Configuration
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setPinchZoom(true);
        lineChart.getLegend().setTextColor(textColor);
        lineChart.getXAxis().setTextColor(textColor);
        lineChart.getAxisLeft().setTextColor(textColor);
        lineChart.getAxisRight().setTextColor(textColor);

        // Bar Chart Configuration
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.getLegend().setTextColor(textColor);
        barChart.getXAxis().setTextColor(textColor);
        barChart.getAxisLeft().setTextColor(textColor);
        barChart.getAxisRight().setTextColor(textColor);
    }

    private void updateReportData() {
        if (allExpensesList.isEmpty()) {
            resetReportViews();
            return;
        }

        // 1. Filter expenses based on selected range
        List<Expense> filtered = filterExpensesByRange(allExpensesList);
        if (filtered.isEmpty()) {
            resetReportViews();
            return;
        }

        // 2. Process Key Metrics
        calculateKeyMetrics(filtered);

        // 3. Render Pie Chart (Categories)
        renderPieChart(filtered);

        // 4. Render Line Chart (Trend)
        renderLineChart(filtered);

        // 5. Render Bar Chart (Payments)
        renderBarChart(filtered);
    }

    private void resetReportViews() {
        tvDailyAverage.setText("₹0.00");
        tvTopCategory.setText("None");
        pieChart.clear();
        lineChart.clear();
        barChart.clear();
    }

    private List<Expense> filterExpensesByRange(List<Expense> list) {
        List<Expense> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        long start = 0;

        String selectedRange = "This Month";
        if (cgFilterRange != null) {
            int checkedId = cgFilterRange.getCheckedChipId();
            if (checkedId == R.id.chip_last_month) {
                selectedRange = "Last Month";
            } else if (checkedId == R.id.chip_this_year) {
                selectedRange = "This Year";
            } else if (checkedId == R.id.chip_all_time) {
                selectedRange = "All Time";
            }
        }

        switch (selectedRange) {
            case "This Month":
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                start = cal.getTimeInMillis();
                break;
            case "Last Month":
                Calendar calStart = Calendar.getInstance();
                calStart.add(Calendar.MONTH, -1);
                calStart.set(Calendar.DAY_OF_MONTH, 1);
                calStart.set(Calendar.HOUR_OF_DAY, 0);
                calStart.set(Calendar.MINUTE, 0);
                calStart.set(Calendar.SECOND, 0);
                start = calStart.getTimeInMillis();

                Calendar calEnd = Calendar.getInstance();
                calEnd.set(Calendar.DAY_OF_MONTH, 1);
                calEnd.add(Calendar.DAY_OF_MONTH, -1);
                calEnd.set(Calendar.HOUR_OF_DAY, 23);
                calEnd.set(Calendar.MINUTE, 59);
                long end = calEnd.getTimeInMillis();

                for (Expense exp : list) {
                    if (exp.getTimestamp() >= start && exp.getTimestamp() <= end) {
                        result.add(exp);
                    }
                }
                return result;
            case "This Year":
                cal.set(Calendar.DAY_OF_YEAR, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                start = cal.getTimeInMillis();
                break;
            case "All Time":
            default:
                return list;
        }

        for (Expense exp : list) {
            if (exp.getTimestamp() >= start && exp.getTimestamp() <= now) {
                result.add(exp);
            }
        }
        return result;
    }

    private void calculateKeyMetrics(List<Expense> list) {
        double sum = 0;
        Map<String, Double> categorySum = new HashMap<>();
        Map<String, Boolean> uniqueDays = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        for (Expense exp : list) {
            sum += exp.getAmount();

            // Group by category
            Double catSum = categorySum.get(exp.getCategoryName());
            categorySum.put(exp.getCategoryName(), (catSum == null ? 0 : catSum) + exp.getAmount());

            // Track unique days
            uniqueDays.put(sdf.format(new Date(exp.getTimestamp())), true);
        }

        // Daily Average
        int daysCount = uniqueDays.size();
        double dailyAvg = daysCount > 0 ? (sum / daysCount) : 0;
        tvDailyAverage.setText(String.format(Locale.getDefault(), "₹%.2f", dailyAvg));

        // Top Category
        String topCat = "None";
        double maxCatVal = -1;
        for (Map.Entry<String, Double> entry : categorySum.entrySet()) {
            if (entry.getValue() > maxCatVal) {
                maxCatVal = entry.getValue();
                topCat = entry.getKey();
            }
        }
        tvTopCategory.setText(String.format("%s (₹%.0f)", topCat, maxCatVal));
    }

    private void renderPieChart(List<Expense> list) {
        Map<String, Double> categorySum = new HashMap<>();
        for (Expense exp : list) {
            Double catSum = categorySum.get(exp.getCategoryName());
            categorySum.put(exp.getCategoryName(), (catSum == null ? 0 : catSum) + exp.getAmount());
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categorySum.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Modern vibrant colors
        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.MATERIAL_COLORS) colors.add(c);
        for (int c : ColorTemplate.VORDIPLOM_COLORS) colors.add(c);
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(10f);
        data.setValueTextColor(getThemeTextColor());

        pieChart.setData(data);
        pieChart.invalidate();
        pieChart.animateY(1000);
    }

    private void renderLineChart(List<Expense> list) {
        // Group expenses by date (sorted)
        Map<Long, Double> dailySum = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Map<Long, String> xLabels = new HashMap<>();

        for (Expense exp : list) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(exp.getTimestamp());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long dayStart = cal.getTimeInMillis();

            Double val = dailySum.get(dayStart);
            dailySum.put(dayStart, (val == null ? 0 : val) + exp.getAmount());
            xLabels.put(dayStart, sdf.format(new Date(dayStart)));
        }

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> xAxisValues = new ArrayList<>();
        int index = 0;

        for (Map.Entry<Long, Double> entry : dailySum.entrySet()) {
            entries.add(new Entry(index, entry.getValue().floatValue()));
            xAxisValues.add(xLabels.get(entry.getKey()));
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Spending");
        dataSet.setColor(Color.parseColor("#1A237E")); // Indigo line
        dataSet.setCircleColor(Color.parseColor("#3F51B5"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(9f);
        dataSet.setValueTextColor(getThemeTextColor());
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#C5CAE9"));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth bezier curve!

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Customize X axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisValues));

        lineChart.invalidate();
        lineChart.animateX(1000);
    }

    private void renderBarChart(List<Expense> list) {
        Map<String, Double> paymentSum = new HashMap<>();
        for (Expense exp : list) {
            Double val = paymentSum.get(exp.getPaymentMethodName());
            paymentSum.put(exp.getPaymentMethodName(), (val == null ? 0 : val) + exp.getAmount());
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> xAxisValues = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Double> entry : paymentSum.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            xAxisValues.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Payments");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextColor(getThemeTextColor());

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisValues));

        barChart.invalidate();
        barChart.animateY(1000);
    }
}
