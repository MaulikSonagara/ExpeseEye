package com.example.expenseeye.theme;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import com.example.expenseeye.R;

public class ThemeManager {

    public enum ThemeColor {
        BACKGROUND, SURFACE, ELEVATED_SURFACE, PRIMARY, PRIMARY_SOFT, SECONDARY, SUCCESS, WARNING, DANGER, TEXT_PRIMARY, TEXT_SECONDARY, DIVIDER
    }

    public static void applyTheme(Activity activity) {
        ThemePreferenceHelper helper = new ThemePreferenceHelper(activity);
        
        // Apply Mode (Only if changed)
        int targetMode = helper.isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
        }
        
        // Apply Style Res
        int themeResId = getThemeResId(helper.getTheme());
        activity.setTheme(themeResId);
    }

    public static int getThemeResId(String themeName) {
        if (ThemePreferenceHelper.THEME_FOREST.equalsIgnoreCase(themeName)) {
            return R.style.Theme_ExpenseEye_ForestLedger;
        } else if (ThemePreferenceHelper.THEME_SAND.equalsIgnoreCase(themeName)) {
            return R.style.Theme_ExpenseEye_MinimalSand;
        } else if (ThemePreferenceHelper.THEME_OCEAN.equalsIgnoreCase(themeName)) {
            return R.style.Theme_ExpenseEye_OceanLedger;
        } else {
            return R.style.Theme_ExpenseEye_MidnightCalm;
        }
    }

    public static int getColor(Context context, ThemeColor colorType) {
        ThemePreferenceHelper helper = new ThemePreferenceHelper(context);
        String theme = helper.getTheme();
        boolean isDark = helper.isDarkMode();

        if (ThemePreferenceHelper.THEME_FOREST.equalsIgnoreCase(theme)) {
            if (isDark) {
                switch (colorType) {
                    case BACKGROUND: return ContextCompat.getColor(context, R.color.fl_d_background);
                    case SURFACE: return ContextCompat.getColor(context, R.color.fl_d_surface);
                    case ELEVATED_SURFACE: return ContextCompat.getColor(context, R.color.fl_d_surface); 
                    case PRIMARY: return ContextCompat.getColor(context, R.color.fl_d_primary);
                    case PRIMARY_SOFT: return ContextCompat.getColor(context, R.color.fl_d_primary); 
                    case SECONDARY: return ContextCompat.getColor(context, R.color.fl_d_secondary);
                    case SUCCESS: return ContextCompat.getColor(context, R.color.mc_d_success);
                    case WARNING: return ContextCompat.getColor(context, R.color.mc_d_warning);
                    case DANGER: return ContextCompat.getColor(context, R.color.fl_d_text_primary); 
                    case TEXT_PRIMARY: return ContextCompat.getColor(context, R.color.fl_d_text_primary);
                    case TEXT_SECONDARY: return ContextCompat.getColor(context, R.color.fl_d_text_secondary);
                    case DIVIDER: return ContextCompat.getColor(context, R.color.fl_d_surface);
                }
            } else {
                switch (colorType) {
                    case BACKGROUND: return ContextCompat.getColor(context, R.color.fl_l_background);
                    case SURFACE: return ContextCompat.getColor(context, R.color.fl_l_surface);
                    case ELEVATED_SURFACE: return ContextCompat.getColor(context, R.color.fl_l_surface);
                    case PRIMARY: return ContextCompat.getColor(context, R.color.fl_l_primary);
                    case PRIMARY_SOFT: return ContextCompat.getColor(context, R.color.fl_l_primary);
                    case SECONDARY: return ContextCompat.getColor(context, R.color.fl_l_secondary);
                    case SUCCESS: return ContextCompat.getColor(context, R.color.mc_l_success);
                    case WARNING: return ContextCompat.getColor(context, R.color.mc_l_warning);
                    case DANGER: return ContextCompat.getColor(context, R.color.mc_l_danger);
                    case TEXT_PRIMARY: return ContextCompat.getColor(context, R.color.fl_l_text_primary);
                    case TEXT_SECONDARY: return ContextCompat.getColor(context, R.color.fl_l_text_secondary);
                    case DIVIDER: return ContextCompat.getColor(context, R.color.fl_l_background);
                }
            }
        } else if (ThemePreferenceHelper.THEME_SAND.equalsIgnoreCase(theme)) {
            if (isDark) {
                switch (colorType) {
                    case BACKGROUND: return ContextCompat.getColor(context, R.color.ms_d_background);
                    case SURFACE: return ContextCompat.getColor(context, R.color.ms_d_surface);
                    case ELEVATED_SURFACE: return ContextCompat.getColor(context, R.color.ms_d_surface);
                    case PRIMARY: return ContextCompat.getColor(context, R.color.ms_d_primary);
                    case PRIMARY_SOFT: return ContextCompat.getColor(context, R.color.ms_d_primary);
                    case SECONDARY: return ContextCompat.getColor(context, R.color.ms_d_secondary);
                    case SUCCESS: return ContextCompat.getColor(context, R.color.mc_d_success);
                    case WARNING: return ContextCompat.getColor(context, R.color.mc_d_warning);
                    case DANGER: return ContextCompat.getColor(context, R.color.mc_d_danger);
                    case TEXT_PRIMARY: return ContextCompat.getColor(context, R.color.ms_d_text_primary);
                    case TEXT_SECONDARY: return ContextCompat.getColor(context, R.color.ms_d_text_secondary);
                    case DIVIDER: return ContextCompat.getColor(context, R.color.ms_d_surface);
                }
            } else {
                switch (colorType) {
                    case BACKGROUND: return ContextCompat.getColor(context, R.color.ms_l_background);
                    case SURFACE: return ContextCompat.getColor(context, R.color.ms_l_surface);
                    case ELEVATED_SURFACE: return ContextCompat.getColor(context, R.color.ms_l_surface);
                    case PRIMARY: return ContextCompat.getColor(context, R.color.ms_l_primary);
                    case PRIMARY_SOFT: return ContextCompat.getColor(context, R.color.ms_l_primary);
                    case SECONDARY: return ContextCompat.getColor(context, R.color.ms_l_secondary);
                    case SUCCESS: return ContextCompat.getColor(context, R.color.mc_l_success);
                    case WARNING: return ContextCompat.getColor(context, R.color.mc_l_warning);
                    case DANGER: return ContextCompat.getColor(context, R.color.mc_l_danger);
                    case TEXT_PRIMARY: return ContextCompat.getColor(context, R.color.ms_l_text_primary);
                    case TEXT_SECONDARY: return ContextCompat.getColor(context, R.color.ms_l_text_secondary);
                    case DIVIDER: return ContextCompat.getColor(context, R.color.ms_l_background);
                }
            }
        } else if (ThemePreferenceHelper.THEME_OCEAN.equalsIgnoreCase(theme)) {
            if (isDark) {
                switch (colorType) {
                    case BACKGROUND: return ContextCompat.getColor(context, R.color.ol_d_background);
                    case SURFACE: return ContextCompat.getColor(context, R.color.ol_d_surface);
                    case ELEVATED_SURFACE: return ContextCompat.getColor(context, R.color.ol_d_surface);
                    case PRIMARY: return ContextCompat.getColor(context, R.color.ol_d_primary);
                    case PRIMARY_SOFT: return ContextCompat.getColor(context, R.color.ol_d_primary);
                    case SECONDARY: return ContextCompat.getColor(context, R.color.ol_d_secondary);
                    case SUCCESS: return ContextCompat.getColor(context, R.color.mc_d_success);
                    case WARNING: return ContextCompat.getColor(context, R.color.mc_d_warning);
                    case DANGER: return ContextCompat.getColor(context, R.color.ol_d_accent);
                    case TEXT_PRIMARY: return ContextCompat.getColor(context, R.color.ol_d_text_primary);
                    case TEXT_SECONDARY: return ContextCompat.getColor(context, R.color.ol_d_text_secondary);
                    case DIVIDER: return ContextCompat.getColor(context, R.color.ol_d_surface);
                }
            } else {
                switch (colorType) {
                    case BACKGROUND: return ContextCompat.getColor(context, R.color.ol_l_background);
                    case SURFACE: return ContextCompat.getColor(context, R.color.ol_l_surface);
                    case ELEVATED_SURFACE: return ContextCompat.getColor(context, R.color.ol_l_surface);
                    case PRIMARY: return ContextCompat.getColor(context, R.color.ol_l_primary);
                    case PRIMARY_SOFT: return ContextCompat.getColor(context, R.color.ol_l_primary);
                    case SECONDARY: return ContextCompat.getColor(context, R.color.ol_l_secondary);
                    case SUCCESS: return ContextCompat.getColor(context, R.color.mc_l_success);
                    case WARNING: return ContextCompat.getColor(context, R.color.mc_l_warning);
                    case DANGER: return ContextCompat.getColor(context, R.color.ol_l_accent);
                    case TEXT_PRIMARY: return ContextCompat.getColor(context, R.color.ol_l_text_primary);
                    case TEXT_SECONDARY: return ContextCompat.getColor(context, R.color.ol_l_text_secondary);
                    case DIVIDER: return ContextCompat.getColor(context, R.color.ol_l_background);
                }
            }
        } else { // Midnight Calm
            if (isDark) {
                switch (colorType) {
                    case BACKGROUND: return ContextCompat.getColor(context, R.color.mc_d_background);
                    case SURFACE: return ContextCompat.getColor(context, R.color.mc_d_surface);
                    case ELEVATED_SURFACE: return ContextCompat.getColor(context, R.color.mc_d_elevated);
                    case PRIMARY: return ContextCompat.getColor(context, R.color.mc_d_primary);
                    case PRIMARY_SOFT: return ContextCompat.getColor(context, R.color.mc_d_primary_soft);
                    case SECONDARY: return ContextCompat.getColor(context, R.color.mc_d_secondary);
                    case SUCCESS: return ContextCompat.getColor(context, R.color.mc_d_success);
                    case WARNING: return ContextCompat.getColor(context, R.color.mc_d_warning);
                    case DANGER: return ContextCompat.getColor(context, R.color.mc_d_danger);
                    case TEXT_PRIMARY: return ContextCompat.getColor(context, R.color.mc_d_text_primary);
                    case TEXT_SECONDARY: return ContextCompat.getColor(context, R.color.mc_d_text_secondary);
                    case DIVIDER: return ContextCompat.getColor(context, R.color.mc_d_divider);
                }
            } else {
                switch (colorType) {
                    case BACKGROUND: return ContextCompat.getColor(context, R.color.mc_l_background);
                    case SURFACE: return ContextCompat.getColor(context, R.color.mc_l_surface);
                    case ELEVATED_SURFACE: return ContextCompat.getColor(context, R.color.mc_l_elevated);
                    case PRIMARY: return ContextCompat.getColor(context, R.color.mc_l_primary);
                    case PRIMARY_SOFT: return ContextCompat.getColor(context, R.color.mc_l_primary_soft);
                    case SECONDARY: return ContextCompat.getColor(context, R.color.mc_l_secondary);
                    case SUCCESS: return ContextCompat.getColor(context, R.color.mc_l_success);
                    case WARNING: return ContextCompat.getColor(context, R.color.mc_l_warning);
                    case DANGER: return ContextCompat.getColor(context, R.color.mc_l_danger);
                    case TEXT_PRIMARY: return ContextCompat.getColor(context, R.color.mc_l_text_primary);
                    case TEXT_SECONDARY: return ContextCompat.getColor(context, R.color.mc_l_text_secondary);
                    case DIVIDER: return ContextCompat.getColor(context, R.color.mc_l_divider);
                }
            }
        }
        return Color.TRANSPARENT;
    }
}
