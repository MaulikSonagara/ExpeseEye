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
            
            if (keyboardHeight > 0) {
                float density = rootView.getResources().getDisplayMetrics().density;
                int gapPx = Math.round(8 * density);

                // With adjustNothing, rootView height is full screen.
                // keyboardTop is the Y coordinate of the top of the keyboard.
                int keyboardTop = rootView.getHeight() - imeBottom;

                // cardBottom is the Y coordinate of the bottom of the card in its normal centered position.
                // We use getTop() + getHeight() which are the layout values.
                int cardBottom = cardView.getTop() + cardView.getHeight();

                // If the card's bottom is below the (keyboard top - gap), we need to move it up.
                int overlap = cardBottom - (keyboardTop - gapPx);
                
                if (overlap > 0) {
                    cardView.setTranslationY(-overlap);
                } else {
                    cardView.setTranslationY(0);
                }
            } else {
                cardView.setTranslationY(0);
            }

            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }
}
