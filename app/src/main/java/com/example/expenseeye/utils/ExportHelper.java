package com.example.expenseeye.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import com.example.expenseeye.models.Expense;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportHelper {

    public static boolean exportToCSV(List<Expense> expenses, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.append("ID,Date,Title,Description,Category,Payment Method,Amount\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            for (Expense expense : expenses) {
                writer.append(String.valueOf(expense.getId())).append(",");
                writer.append(sdf.format(new Date(expense.getTimestamp()))).append(",");
                writer.append(escapeCSV(expense.getTitle())).append(",");
                writer.append(escapeCSV(expense.getDescription())).append(",");
                writer.append(escapeCSV(expense.getCategoryName())).append(",");
                writer.append(escapeCSV(expense.getPaymentMethodName())).append(",");
                writer.append(String.valueOf(expense.getAmount())).append("\n");
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
        int y = 40;
        int pageNum = 1;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(12);

        // Header
        paint.setColor(Color.parseColor("#3F51B5")); // Indigo
        canvas.drawRect(0, 0, pageWidth, 60, paint);

        Paint headerTextPaint = new Paint();
        headerTextPaint.setColor(Color.WHITE);
        headerTextPaint.setTextSize(18);
        headerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("ExpenseEye - Expense History Report", 20, 38, headerTextPaint);

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        Paint dateTextPaint = new Paint();
        dateTextPaint.setColor(Color.WHITE);
        dateTextPaint.setTextSize(10);
        canvas.drawText("Generated on: " + sdf.format(new Date()), pageWidth - 180, 38, dateTextPaint);

        y = 90;

        // Table Header Background
        paint.setColor(Color.parseColor("#EEEEEE"));
        canvas.drawRect(20, y - 15, pageWidth - 20, y + 10, paint);

        // Table Headers
        Paint tableHeaderPaint = new Paint();
        tableHeaderPaint.setColor(Color.BLACK);
        tableHeaderPaint.setTextSize(10);
        tableHeaderPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Date & Time", 30, y, tableHeaderPaint);
        canvas.drawText("Title", 140, y, tableHeaderPaint);
        canvas.drawText("Category", 300, y, tableHeaderPaint);
        canvas.drawText("Payment", 410, y, tableHeaderPaint);
        canvas.drawText("Amount", 500, y, tableHeaderPaint);

        y += 25;
        double total = 0.0;

        for (Expense expense : expenses) {
            if (y > pageHeight - 60) {
                // Next Page
                document.finishPage(page);
                pageNum++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;

                // Re-draw table header
                paint.setColor(Color.parseColor("#EEEEEE"));
                canvas.drawRect(20, y - 15, pageWidth - 20, y + 10, paint);
                canvas.drawText("Date & Time", 30, y, tableHeaderPaint);
                canvas.drawText("Title", 140, y, tableHeaderPaint);
                canvas.drawText("Category", 300, y, tableHeaderPaint);
                canvas.drawText("Payment", 410, y, tableHeaderPaint);
                canvas.drawText("Amount", 500, y, tableHeaderPaint);
                y += 25;
            }

            canvas.drawText(sdf.format(new Date(expense.getTimestamp())), 30, y, textPaint);
            
            String title = expense.getTitle();
            if (title.length() > 22) title = title.substring(0, 20) + "..";
            canvas.drawText(title, 140, y, textPaint);
            
            canvas.drawText(expense.getCategoryName(), 300, y, textPaint);
            canvas.drawText(expense.getPaymentMethodName(), 410, y, textPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", expense.getAmount()), 500, y, textPaint);

            total += expense.getAmount();
            y += 20;

            paint.setColor(Color.parseColor("#DDDDDD"));
            canvas.drawLine(20, y - 12, pageWidth - 20, y - 12, paint);
        }

        // Draw Total
        if (y > pageHeight - 60) {
            document.finishPage(page);
            pageNum++;
            pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            y = 50;
        }

        paint.setColor(Color.parseColor("#E8EAF6"));
        canvas.drawRect(20, y - 15, pageWidth - 20, y + 15, paint);

        Paint totalPaint = new Paint();
        totalPaint.setColor(Color.parseColor("#3F51B5"));
        totalPaint.setTextSize(12);
        totalPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Total Spend:", 30, y + 5, totalPaint);
        canvas.drawText(String.format(Locale.getDefault(), "%.2f", total), 500, y + 5, totalPaint);

        document.finishPage(page);

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
}
