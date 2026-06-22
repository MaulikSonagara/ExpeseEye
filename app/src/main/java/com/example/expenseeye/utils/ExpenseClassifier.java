package com.example.expenseeye.utils;

import java.util.Locale;

public class ExpenseClassifier {

    public static String classifyExpense(String title) {
        if (title == null) return "Other";
        String lowerTitle = title.toLowerCase(Locale.ROOT);

        // Predefined keyword matching
        if (containsAny(lowerTitle, "milk", "vegetables", "rice", "bread", "grocery", "groceries", "fruits", "oil", "food", "egg", "chicken", "meat")) {
            return "Groceries";
        }
        if (containsAny(lowerTitle, "electricity", "light bill", "power bill", "current bill")) {
            return "Electricity";
        }
        if (containsAny(lowerTitle, "water", "water bill", "tap water", "water tanker")) {
            return "Water Bill";
        }
        if (containsAny(lowerTitle, "gas", "gas cylinder", "lpg", "indane", "hp gas", "gas bill")) {
            return "Gas";
        }
        if (containsAny(lowerTitle, "rent", "room rent", "house rent", "pg rent")) {
            return "Rent";
        }
        if (containsAny(lowerTitle, "internet", "wifi", "broadband", "recharge", "jio", "airtel", "fiber")) {
            return "Internet";
        }
        if (containsAny(lowerTitle, "medicine", "doctor", "hospital", "clinic", "tablet", "pharma", "prescription", "health")) {
            return "Medical";
        }
        if (containsAny(lowerTitle, "petrol", "diesel", "transport", "cab", "uber", "ola", "bus", "auto", "train", "flight", "fuel", "travel")) {
            return "Transport";
        }
        if (containsAny(lowerTitle, "shopping", "clothes", "shirt", "pant", "amazon", "flipkart", "myntra", "mall", "shoes")) {
            return "Shopping";
        }
        if (containsAny(lowerTitle, "movie", "netflix", "popcorn", "theatre", "cinema", "entertainment", "club", "game", "hotstar")) {
            return "Entertainment";
        }
        if (containsAny(lowerTitle, "education", "school", "college", "book", "fees", "tution", "course", "pen", "exam")) {
            return "Education";
        }

        return "Other"; // Default fallback
    }

    public static String classifyChecklistItem(String title) {
        if (title == null) return "Other";
        String lowerTitle = title.toLowerCase(Locale.ROOT);

        if (containsAny(lowerTitle, "milk", "cheese", "paneer", "butter", "curd", "yogurt", "dairy", "cream")) {
            return "Dairy";
        }
        if (containsAny(lowerTitle, "rice", "wheat", "dal", "flour", "sugar", "salt", "oil", "groceries", "grocery", "spices", "tea", "coffee")) {
            return "Grocery";
        }
        if (containsAny(lowerTitle, "toothpaste", "brush", "soap", "shampoo", "detergent", "face wash", "body wash", "lotion", "comb", "oil")) {
            return "Personal Care";
        }

        return "Other";
    }

    private static boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
