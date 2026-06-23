package com.example.expenseeye.theme;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemePreferenceHelper {
    private static final String PREF_NAME = "ExpenseEyePrefs";
    private static final String KEY_THEME = "selected_theme_v2";
    private static final String KEY_MODE = "selected_mode_v2";

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
}
