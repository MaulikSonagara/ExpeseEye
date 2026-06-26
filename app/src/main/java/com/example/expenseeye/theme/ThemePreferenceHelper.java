package com.example.expenseeye.theme;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemePreferenceHelper {
    private static final String PREF_NAME = "ExpenseEyePrefs";
    private static final String KEY_THEME = "selected_theme_v2";
    private static final String KEY_MODE = "selected_mode_v2";
    private static final String KEY_SUGGESTIONS = "title_suggestions_enabled";
    private static final String KEY_CURRENCY = "currency_symbol";
    private static final String KEY_DEFAULT_PAYMENT = "default_payment_method";
    private static final String KEY_SMART_CLASSIFIER = "smart_classifier_enabled";
    private static final String KEY_CHECKLIST_SUGGESTIONS = "checklist_suggestions_enabled";
    private static final String KEY_CHECKLIST_SMART_CLASSIFIER = "checklist_classifier_enabled";

    public static final String THEME_MIDNIGHT = "Midnight Calm";
    public static final String THEME_FOREST = "Forest Ledger";
    public static final String THEME_SAND = "Minimal Sand";
    public static final String THEME_OCEAN = "Ocean Ledger";

    public static final String MODE_LIGHT = "Light";
    public static final String MODE_DARK = "Dark";

    private final SharedPreferences prefs;

    public ThemePreferenceHelper(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setTheme(String themeName) {
        prefs.edit().putString(KEY_THEME, themeName).commit();
    }

    public String getTheme() {
        return prefs.getString(KEY_THEME, THEME_MIDNIGHT);
    }

    public void setMode(String modeName) {
        prefs.edit().putString(KEY_MODE, modeName).commit();
    }

    public String getMode() {
        return prefs.getString(KEY_MODE, MODE_DARK);
    }

    public boolean isDarkMode() {
        return MODE_DARK.equalsIgnoreCase(getMode());
    }

    public void setTitleSuggestionsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SUGGESTIONS, enabled).apply();
    }

    public boolean isTitleSuggestionsEnabled() {
        return prefs.getBoolean(KEY_SUGGESTIONS, true);
    }

    public void setCurrencySymbol(String symbol) {
        prefs.edit().putString(KEY_CURRENCY, symbol).apply();
    }

    public String getCurrencySymbol() {
        return prefs.getString(KEY_CURRENCY, "₹");
    }

    public void setDefaultPaymentMethodId(int id) {
        prefs.edit().putInt(KEY_DEFAULT_PAYMENT, id).apply();
    }

    public int getDefaultPaymentMethodId() {
        return prefs.getInt(KEY_DEFAULT_PAYMENT, -1);
    }

    public void setSmartClassifierEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SMART_CLASSIFIER, enabled).apply();
    }

    public boolean isSmartClassifierEnabled() {
        return prefs.getBoolean(KEY_SMART_CLASSIFIER, true);
    }

    public void setChecklistTitleSuggestionsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CHECKLIST_SUGGESTIONS, enabled).apply();
    }

    public boolean isChecklistTitleSuggestionsEnabled() {
        return prefs.getBoolean(KEY_CHECKLIST_SUGGESTIONS, true);
    }

    public void setChecklistSmartClassifierEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CHECKLIST_SMART_CLASSIFIER, enabled).apply();
    }

    public boolean isChecklistSmartClassifierEnabled() {
        return prefs.getBoolean(KEY_CHECKLIST_SMART_CLASSIFIER, true);
    }

    public void setShoppingModeEnabled(boolean enabled) {
        prefs.edit().putBoolean("shopping_mode_enabled", enabled).apply();
    }

    public boolean isShoppingModeEnabled() {
        return prefs.getBoolean("shopping_mode_enabled", false);
    }
}
