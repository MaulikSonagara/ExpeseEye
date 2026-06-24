package com.example.expenseeye.utils;

import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.CategoryKeyword;
import java.util.List;
import java.util.Locale;

public class ExpenseClassifier {

    public static String classifyExpense(String text, List<Category> activeCategories, List<CategoryKeyword> allKeywords) {
        if (text == null || text.trim().isEmpty() || activeCategories == null || activeCategories.isEmpty()) {
            return "Others";
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        Category bestCategory = null;
        int bestMatchScore = 0;

        for (Category category : activeCategories) {
            if (!category.isEnabled()) {
                continue;
            }

            int categoryId = category.getId();
            int matchScore = 0;

            if (allKeywords != null) {
                for (CategoryKeyword kw : allKeywords) {
                    if (kw.getCategoryId() != categoryId) {
                        continue;
                    }

                    String keyword = kw.getKeyword().toLowerCase(Locale.ROOT);
                    if (keyword.isEmpty()) {
                        continue;
                    }

                    if (lowerText.equals(keyword)) {
                        matchScore += 10;
                    } else if (lowerText.contains(keyword)) {
                        matchScore += 2;
                        // Add weight to longer keyword match
                        matchScore += keyword.length() / 2;
                    }
                }
            }

            if (matchScore > bestMatchScore) {
                bestMatchScore = matchScore;
                bestCategory = category;
            }
        }

        if (bestCategory != null && bestMatchScore > 0) {
            return bestCategory.getName();
        }

        // Fallback: search for a category named "Others" or "Other"
        for (Category category : activeCategories) {
            if (category.getName().equalsIgnoreCase("Others") || category.getName().equalsIgnoreCase("Other")) {
                return category.getName();
            }
        }

        return "Others";
    }

    public static String classifyExpense(String title) {
        if (title == null) return "Other";
        String lowerTitle = title.toLowerCase(Locale.ROOT);

        if (containsAny(lowerTitle, "milk", "vegetables", "rice", "bread", "grocery", "groceries", "fruits", "oil", "food", "egg", "chicken", "meat", "lunch", "dinner", "restaurant", "cafe", "snacks")) {
            return "Food";
        }
        if (containsAny(lowerTitle, "electricity", "light bill", "power bill", "current bill", "water", "gas", "rent", "internet", "wifi", "broadband", "recharge", "bill")) {
            return "Bills";
        }
        if (containsAny(lowerTitle, "medicine", "doctor", "hospital", "clinic", "tablet", "pharma", "health")) {
            return "Health";
        }
        if (containsAny(lowerTitle, "petrol", "diesel", "transport", "cab", "uber", "ola", "bus", "auto", "train", "flight", "fuel", "travel")) {
            return "Travel";
        }
        if (containsAny(lowerTitle, "shopping", "clothes", "shirt", "pant", "amazon", "flipkart", "myntra", "mall", "shoes")) {
            return "Shopping";
        }
        if (containsAny(lowerTitle, "movie", "netflix", "popcorn", "theatre", "cinema", "entertainment", "club", "game")) {
            return "Entertainment";
        }
        if (containsAny(lowerTitle, "education", "school", "college", "book", "fees", "tution", "course")) {
            return "Education";
        }
        if (containsAny(lowerTitle, "salary", "income", "paycheck")) {
            return "Salary";
        }
        if (containsAny(lowerTitle, "investment", "stock", "mutual fund", "sip", "shares")) {
            return "Investment";
        }

        return "Others";
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
