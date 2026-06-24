package com.example.expenseeye.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import com.example.expenseeye.models.Expense;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExportHelper {

    public static boolean exportToCSV(List<Expense> expenses, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            // Write CSV headers as requested: Date, Title, Category, Payment Method, Amount, Notes
            writer.append("Date,Title,Category,Payment Method,Amount,Notes\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            for (Expense expense : expenses) {
                writer.append(sdf.format(new Date(expense.getTimestamp()))).append(",");
                writer.append(escapeCSV(expense.getTitle())).append(",");
                writer.append(escapeCSV(expense.getCategoryName())).append(",");
                writer.append(escapeCSV(expense.getPaymentMethodName())).append(",");
                writer.append(String.valueOf(expense.getAmount())).append(",");
                writer.append(escapeCSV(expense.getDescription())).append("\n"); // Description is used for Notes
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static boolean exportToPDF(List<Expense> expenses, File file) {
        PdfDocument document = new PdfDocument();
        int pageWidth = 595; // A4 size width
        int pageHeight = 842; // A4 size height
        
        // --- 1. Compute Statistics ---
        double totalExpenses = 0.0;
        double thisMonthTotal = 0.0;
        double thisWeekTotal = 0.0;
        
        Calendar calMonth = Calendar.getInstance();
        calMonth.set(Calendar.DAY_OF_MONTH, 1);
        calMonth.set(Calendar.HOUR_OF_DAY, 0);
        calMonth.set(Calendar.MINUTE, 0);
        calMonth.set(Calendar.SECOND, 0);
        calMonth.set(Calendar.MILLISECOND, 0);
        long startOfMonth = calMonth.getTimeInMillis();

        Calendar calWeek = Calendar.getInstance();
        calWeek.set(Calendar.DAY_OF_WEEK, calWeek.getFirstDayOfWeek());
        calWeek.set(Calendar.HOUR_OF_DAY, 0);
        calWeek.set(Calendar.MINUTE, 0);
        calWeek.set(Calendar.SECOND, 0);
        calWeek.set(Calendar.MILLISECOND, 0);
        long startOfWeek = calWeek.getTimeInMillis();

        Map<String, Double> categoryMap = new HashMap<>();
        Map<String, Double> paymentMap = new HashMap<>();
        
        long oldestTimestamp = Long.MAX_VALUE;
        long newestTimestamp = Long.MIN_VALUE;

        for (Expense e : expenses) {
            double amt = e.getAmount();
            totalExpenses += amt;
            
            if (e.getTimestamp() >= startOfMonth) {
                thisMonthTotal += amt;
            }
            if (e.getTimestamp() >= startOfWeek) {
                thisWeekTotal += amt;
            }
            
            categoryMap.put(e.getCategoryName(), categoryMap.getOrDefault(e.getCategoryName(), 0.0) + amt);
            paymentMap.put(e.getPaymentMethodName(), paymentMap.getOrDefault(e.getPaymentMethodName(), 0.0) + amt);
            
            if (e.getTimestamp() < oldestTimestamp) oldestTimestamp = e.getTimestamp();
            if (e.getTimestamp() > newestTimestamp) newestTimestamp = e.getTimestamp();
        }

        String topCategory = "N/A";
        double maxCategorySpend = -1.0;
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            if (entry.getValue() > maxCategorySpend) {
                maxCategorySpend = entry.getValue();
                topCategory = entry.getKey();
            }
        }
        int totalCategoriesUsed = categoryMap.size();

        SimpleDateFormat rangeFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String dateRangeStr;
        if (expenses.isEmpty()) {
            dateRangeStr = "No data available";
        } else {
            dateRangeStr = rangeFormat.format(new Date(oldestTimestamp)) + " - " + rangeFormat.format(new Date(newestTimestamp));
        }

        SimpleDateFormat genFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        String genDateStr = genFormat.format(new Date());

        int expensesPerPage = 22;
        int totalPages = 1 + (int) Math.ceil((double) expenses.size() / expensesPerPage);
        if (expenses.isEmpty()) {
            totalPages = 1;
        }

        // --- 2. Page 1: Statistics Summary ---
        PdfDocument.PageInfo pageInfo1 = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page1 = document.startPage(pageInfo1);
        Canvas canvas1 = page1.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Header Background Banner
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#5B67F5")); // Midnight Calm Primary
        canvas1.drawRoundRect(new RectF(20, 20, pageWidth - 20, 90), 12, 12, paint);

        // Header Titles
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(16);
        canvas1.drawText("EXPENSEEYE FINANCIAL REPORT", 35, 50, paint);
        
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paint.setTextSize(10);
        canvas1.drawText("Period: " + dateRangeStr, 35, 72, paint);
        canvas1.drawText("Generated: " + genDateStr, pageWidth - 160, 50, paint);

        // Grid KPI Cards
        // Card 1: Total Expenses
        float cY = 110;
        float cH = 60;
        float cW = 175;
        
        drawKPICard(canvas1, 20, cY, cW, cH, "TOTAL EXPENSES", String.format(Locale.getDefault(), "₹%.2f", totalExpenses), "#DC2626");
        drawKPICard(canvas1, 210, cY, cW, cH, "TOTAL INCOME", "N/A", "#64748B");
        drawKPICard(canvas1, 400, cY, cW, cH, "NET BALANCE", String.format(Locale.getDefault(), "-₹%.2f", totalExpenses), "#DC2626");

        // Time Frame Totals
        drawKPICard(canvas1, 20, 185, 267, cH, "THIS MONTH TOTAL", String.format(Locale.getDefault(), "₹%.2f", thisMonthTotal), "#5B67F5");
        drawKPICard(canvas1, 307, 185, 268, cH, "THIS WEEK TOTAL", String.format(Locale.getDefault(), "₹%.2f", thisWeekTotal), "#14B8A6");

        // Draw Left Column: Insights Table
        float yOffset = 270;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(12);
        canvas1.drawText("Quick Insights", 25, yOffset, paint);
        
        yOffset += 20;
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paint.setTextSize(9);
        canvas1.drawText("Top Spending Category:", 25, yOffset, paint);
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(10);
        canvas1.drawText(topCategory + " (" + String.format(Locale.getDefault(), "₹%.2f", maxCategorySpend >= 0 ? maxCategorySpend : 0.0) + ")", 145, yOffset, paint);

        yOffset += 18;
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paint.setTextSize(9);
        canvas1.drawText("Total Categories Used:", 25, yOffset, paint);
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(10);
        canvas1.drawText(String.valueOf(totalCategoriesUsed), 145, yOffset, paint);

        // Payment Methods Summary (List top 3)
        yOffset += 30;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(12);
        canvas1.drawText("Payment Methods Breakdown", 25, yOffset, paint);

        yOffset += 15;
        paint.setColor(Color.parseColor("#CBD5E1"));
        canvas1.drawLine(20, yOffset, 290, yOffset, paint);

        List<Map.Entry<String, Double>> sortedPayments = new ArrayList<>(paymentMap.entrySet());
        Collections.sort(sortedPayments, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        
        yOffset += 15;
        paint.setTextSize(9);
        if (sortedPayments.isEmpty()) {
            paint.setColor(Color.parseColor("#64748B"));
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC));
            canvas1.drawText("No payments registered", 25, yOffset, paint);
        } else {
            for (int i = 0; i < Math.min(4, sortedPayments.size()); i++) {
                Map.Entry<String, Double> entry = sortedPayments.get(i);
                double pct = totalExpenses > 0 ? (entry.getValue() / totalExpenses * 100) : 0.0;
                
                paint.setColor(Color.parseColor("#0F172A"));
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                canvas1.drawText(entry.getKey(), 25, yOffset, paint);
                
                paint.setColor(Color.parseColor("#64748B"));
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
                String paymentStr = String.format(Locale.getDefault(), "₹%.2f (%.1f%%)", entry.getValue(), pct);
                canvas1.drawText(paymentStr, 160, yOffset, paint);
                yOffset += 18;
            }
        }

        // Draw Right Column: Donut Pie Chart & Legend
        float chartCenterX = 440;
        float chartCenterY = 345;
        float chartRadius = 60;
        RectF chartRect = new RectF(chartCenterX - chartRadius, chartCenterY - chartRadius, chartCenterX + chartRadius, chartCenterY + chartRadius);
        
        int[] chartColors = {
            Color.parseColor("#5B67F5"), Color.parseColor("#14B8A6"), Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"), Color.parseColor("#A855F7"), Color.parseColor("#06B6D4"),
            Color.parseColor("#EC4899"), Color.parseColor("#F97316"), Color.parseColor("#9E9E9E")
        };

        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryMap.entrySet());
        Collections.sort(sortedCategories, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        if (totalExpenses > 0 && !sortedCategories.isEmpty()) {
            float startAngle = -90f;
            int cIndex = 0;
            for (Map.Entry<String, Double> entry : sortedCategories) {
                float sweepAngle = (float) (entry.getValue() / totalExpenses * 360f);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(chartColors[cIndex % chartColors.length]);
                canvas1.drawArc(chartRect, startAngle, sweepAngle, true, paint);
                startAngle += sweepAngle;
                cIndex++;
            }
            // Draw Donut Hole
            paint.setColor(Color.WHITE);
            canvas1.drawCircle(chartCenterX, chartCenterY, chartRadius - 22, paint);
        } else {
            // Empty Chart Outline
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(12);
            paint.setColor(Color.parseColor("#E2E8F0"));
            canvas1.drawCircle(chartCenterX, chartCenterY, chartRadius - 6, paint);
            
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#64748B"));
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            paint.setTextSize(8);
            canvas1.drawText("NO DATA", chartCenterX - 18, chartCenterY + 3, paint);
        }

        // Draw Legend for Categories (Top 5)
        float legendX = 330;
        float legendY = 430;
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(8);
        for (int i = 0; i < Math.min(5, sortedCategories.size()); i++) {
            Map.Entry<String, Double> entry = sortedCategories.get(i);
            double pct = totalExpenses > 0 ? (entry.getValue() / totalExpenses * 100) : 0.0;
            
            // Color box
            paint.setColor(chartColors[i % chartColors.length]);
            canvas1.drawRoundRect(new RectF(legendX, legendY - 6, legendX + 8, legendY + 2), 2, 2, paint);
            
            // Text Label
            paint.setColor(Color.parseColor("#0F172A"));
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            String nameText = entry.getKey();
            if (nameText.length() > 12) nameText = nameText.substring(0, 10) + "..";
            canvas1.drawText(nameText, legendX + 14, legendY, paint);
            
            // Percentage
            paint.setColor(Color.parseColor("#64748B"));
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            canvas1.drawText(String.format(Locale.getDefault(), "%.1f%%", pct), legendX + 78, legendY, paint);
            
            legendY += 14;
        }

        // Bottom Table: Category Expense Distribution List
        float tableY = 530;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(12);
        canvas1.drawText("Expense Distribution Summary", 20, tableY, paint);

        tableY += 15;
        // Table Header
        paint.setColor(Color.parseColor("#F1F5F9"));
        canvas1.drawRoundRect(new RectF(20, tableY, pageWidth - 20, tableY + 20), 4, 4, paint);
        
        paint.setColor(Color.parseColor("#475569"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        canvas1.drawText("Category", 30, tableY + 13, paint);
        canvas1.drawText("Total Spend", 250, tableY + 13, paint);
        canvas1.drawText("Percentage", 450, tableY + 13, paint);
        
        tableY += 20;
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        
        if (sortedCategories.isEmpty()) {
            tableY += 20;
            paint.setColor(Color.parseColor("#64748B"));
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC));
            canvas1.drawText("No expenses logged during this period", 30, tableY, paint);
        } else {
            for (int i = 0; i < Math.min(8, sortedCategories.size()); i++) {
                tableY += 18;
                Map.Entry<String, Double> entry = sortedCategories.get(i);
                double pct = totalExpenses > 0 ? (entry.getValue() / totalExpenses * 100) : 0.0;
                
                paint.setColor(Color.parseColor("#0F172A"));
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                canvas1.drawText(entry.getKey(), 30, tableY, paint);
                
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
                canvas1.drawText(String.format(Locale.getDefault(), "₹%.2f", entry.getValue()), 250, tableY, paint);
                
                paint.setColor(Color.parseColor("#64748B"));
                canvas1.drawText(String.format(Locale.getDefault(), "%.2f%%", pct), 450, tableY, paint);
                
                paint.setColor(Color.parseColor("#F1F5F9"));
                canvas1.drawLine(20, tableY + 4, pageWidth - 20, tableY + 4, paint);
            }
        }

        // Page 1 Footer
        drawFooter(canvas1, pageWidth, pageHeight, 1, totalPages);
        document.finishPage(page1);

        // --- 3. Page 2 onward: Expense List ---
        int expenseIndex = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        
        for (int pNum = 2; pNum <= totalPages; pNum++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pNum).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Table Title / Header
            paint.setColor(Color.parseColor("#0F172A"));
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            paint.setTextSize(13);
            canvas.drawText("Detailed Expense Log", 20, 45, paint);

            // Period dates smaller
            paint.setColor(Color.parseColor("#64748B"));
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            paint.setTextSize(9);
            canvas.drawText("Items " + (expenseIndex + 1) + " - " + Math.min(expenseIndex + expensesPerPage, expenses.size()) + " of " + expenses.size(), 20, 60, paint);

            float rowY = 80;
            // Table Column Headers Background
            paint.setColor(Color.parseColor("#F1F5F9"));
            canvas.drawRoundRect(new RectF(20, rowY, pageWidth - 20, rowY + 22), 4, 4, paint);

            // Table Headers
            paint.setColor(Color.parseColor("#475569"));
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            paint.setTextSize(9);
            
            canvas.drawText("Date & Time", 30, rowY + 14, paint);
            canvas.drawText("Title & Description", 130, rowY + 14, paint);
            canvas.drawText("Category", 310, rowY + 14, paint);
            canvas.drawText("Payment", 420, rowY + 14, paint);
            canvas.drawText("Amount", 510, rowY + 14, paint);

            rowY += 22;

            int itemsOnThisPage = 0;
            while (expenseIndex < expenses.size() && itemsOnThisPage < expensesPerPage) {
                rowY += 28;
                Expense expense = expenses.get(expenseIndex);

                // Date
                paint.setColor(Color.parseColor("#0F172A"));
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
                paint.setTextSize(8.5f);
                canvas.drawText(sdf.format(new Date(expense.getTimestamp())), 30, rowY, paint);

                // Title & Description (Stack them)
                String title = expense.getTitle();
                if (title.length() > 28) title = title.substring(0, 26) + "..";
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                canvas.drawText(title, 130, rowY - 4, paint);

                String desc = expense.getDescription();
                if (desc == null || desc.trim().isEmpty()) desc = "-";
                if (desc.length() > 34) desc = desc.substring(0, 32) + "..";
                paint.setColor(Color.parseColor("#64748B"));
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
                paint.setTextSize(7.5f);
                canvas.drawText(desc, 130, rowY + 6, paint);

                // Category
                paint.setColor(Color.parseColor("#0F172A"));
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
                paint.setTextSize(8.5f);
                String cat = expense.getCategoryName();
                if (cat.length() > 16) cat = cat.substring(0, 14) + "..";
                canvas.drawText(cat, 310, rowY, paint);

                // Payment Method
                String pay = expense.getPaymentMethodName();
                if (pay.length() > 14) pay = pay.substring(0, 12) + "..";
                canvas.drawText(pay, 420, rowY, paint);

                // Amount
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                canvas.drawText(String.format(Locale.getDefault(), "₹%.2f", expense.getAmount()), 510, rowY, paint);

                // Draw thin divider line
                paint.setColor(Color.parseColor("#E2E8F0"));
                canvas.drawLine(20, rowY + 12, pageWidth - 20, rowY + 12, paint);

                expenseIndex++;
                itemsOnThisPage++;
            }

            // Draw total spend on the final page of details
            if (expenseIndex >= expenses.size()) {
                rowY += 28;
                paint.setColor(Color.parseColor("#F8FAFC"));
                canvas.drawRoundRect(new RectF(20, rowY - 14, pageWidth - 20, rowY + 14), 6, 6, paint);
                
                paint.setColor(Color.parseColor("#5B67F5"));
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                paint.setTextSize(10);
                canvas.drawText("Total Detailed Expenses Logged:", 30, rowY + 4, paint);
                canvas.drawText(String.format(Locale.getDefault(), "₹%.2f", totalExpenses), 500, rowY + 4, paint);
            }

            // Page Footer
            drawFooter(canvas, pageWidth, pageHeight, pNum, totalPages);
            document.finishPage(page);
        }

        // Write document to file
        try {
            document.writeTo(new FileOutputStream(file));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            document.close();
        }
    }

    private static void drawKPICard(Canvas canvas, float x, float y, float w, float h, String title, String val, String valColorHex) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        
        // Card Body
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRoundRect(new RectF(x, y, x + w, y + h), 8, 8, p);
        
        // Card Border
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        p.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawRoundRect(new RectF(x, y, x + w, y + h), 8, 8, p);
        
        // Label
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.parseColor("#64748B"));
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        p.setTextSize(7.5f);
        canvas.drawText(title, x + 10, y + 20, p);
        
        // Value
        p.setColor(Color.parseColor(valColorHex));
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        p.setTextSize(12);
        canvas.drawText(val, x + 10, y + 42, p);
    }

    private static void drawFooter(Canvas canvas, int pageWidth, int pageHeight, int pageNum, int totalPages) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.parseColor("#94A3B8"));
        p.setTextSize(8);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        
        // Divider
        canvas.drawLine(20, pageHeight - 40, pageWidth - 20, pageHeight - 40, p);
        
        // App name & page numbers
        canvas.drawText("ExpenseEye - Smart Personal Expense Tracker", 20, pageHeight - 25, p);
        String pageStr = "Page " + pageNum + " of " + totalPages;
        canvas.drawText(pageStr, pageWidth - 20 - p.measureText(pageStr), pageHeight - 25, p);
    }
}
