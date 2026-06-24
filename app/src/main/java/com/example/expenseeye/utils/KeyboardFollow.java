package com.example.expenseeye.utils;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class KeyboardFollow {

    public static void attach(@NonNull View rootView, @NonNull View cardView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            // keyboardHeight is the distance from the bottom of the screen that the keyboard occupies
            int keyboardHeight = Math.max(imeBottom - systemBarsBottom, 0);

            // Apply padding to the root view instead of translation to the card
            // This allows NestedScrollView to handle the content properly
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), keyboardHeight);

            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }
}
