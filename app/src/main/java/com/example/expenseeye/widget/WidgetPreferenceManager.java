package com.example.expenseeye.widget;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class WidgetPreferenceManager {
    private static final String PREF_NAME = "WidgetPrefs";
    private static final String KEY_ACTION_PREFIX = "quick_action_";

    public static List<String> getQuickActions(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<String> list = new ArrayList<>();
        
        // Defaults to Groceries, Transport, Shopping if not set
        String action1 = prefs.getString(KEY_ACTION_PREFIX + "0", "Groceries");
        String action2 = prefs.getString(KEY_ACTION_PREFIX + "1", "Transport");
        String action3 = prefs.getString(KEY_ACTION_PREFIX + "2", "Shopping");

        if (action1 != null && !action1.trim().isEmpty()) {
            list.add(action1);
        }
        if (action2 != null && !action2.trim().isEmpty()) {
            list.add(action2);
        }
        if (action3 != null && !action3.trim().isEmpty()) {
            list.add(action3);
        }
        
        return list;
    }

    public static void saveQuickActions(Context context, List<String> categories) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Clear previous keys
        editor.remove(KEY_ACTION_PREFIX + "0");
        editor.remove(KEY_ACTION_PREFIX + "1");
        editor.remove(KEY_ACTION_PREFIX + "2");
        
        for (int i = 0; i < categories.size() && i < 3; i++) {
            editor.putString(KEY_ACTION_PREFIX + i, categories.get(i));
        }
        editor.apply();
    }
}
