package com.example.expenseeye.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expenseeye.R;
import com.example.expenseeye.theme.ThemePreferenceHelper;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ThemeSelectionAdapter extends RecyclerView.Adapter<ThemeSelectionAdapter.ViewHolder> {

    public interface OnThemeClickListener {
        void onThemeClick(String themeName);
    }

    private final List<String> themes;
    private final String selectedTheme;
    private final OnThemeClickListener listener;

    public ThemeSelectionAdapter(List<String> themes, String selectedTheme, OnThemeClickListener listener) {
        this.themes = themes;
        this.selectedTheme = selectedTheme;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme_preview, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String theme = themes.get(position);
        holder.tvName.setText(theme);

        Context ctx = holder.itemView.getContext();
        ThemePreferenceHelper themeHelper = new ThemePreferenceHelper(ctx);
        boolean isDark = themeHelper.isDarkMode();

        int p, s, b, a;

        if (ThemePreferenceHelper.THEME_FOREST.equalsIgnoreCase(theme)) {
            if (isDark) {
                p = R.color.fl_d_primary; s = R.color.fl_d_secondary; b = R.color.fl_d_background; a = R.color.mc_d_warning;
            } else {
                p = R.color.fl_l_primary; s = R.color.fl_l_secondary; b = R.color.fl_l_background; a = R.color.mc_l_warning;
            }
        } else if (ThemePreferenceHelper.THEME_SAND.equalsIgnoreCase(theme)) {
            if (isDark) {
                p = R.color.ms_d_primary; s = R.color.ms_d_secondary; b = R.color.ms_d_background; a = R.color.mc_d_danger;
            } else {
                p = R.color.ms_l_primary; s = R.color.ms_l_secondary; b = R.color.ms_l_background; a = R.color.mc_l_danger;
            }
        } else if (ThemePreferenceHelper.THEME_OCEAN.equalsIgnoreCase(theme)) {
            if (isDark) {
                p = R.color.ol_d_primary; s = R.color.ol_d_secondary; b = R.color.ol_d_background; a = R.color.ol_d_accent;
            } else {
                p = R.color.ol_l_primary; s = R.color.ol_l_secondary; b = R.color.ol_l_background; a = R.color.ol_l_accent;
            }
        } else { // Midnight Calm
            if (isDark) {
                p = R.color.mc_d_primary; s = R.color.mc_d_secondary; b = R.color.mc_d_background; a = R.color.mc_d_success;
            } else {
                p = R.color.mc_l_primary; s = R.color.mc_l_secondary; b = R.color.mc_l_background; a = R.color.mc_l_success;
            }
        }

        // Set colors dynamically to custom dots
        holder.v1.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, p)));
        holder.v2.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, s)));
        holder.v3.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, b)));
        holder.v4.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, a)));

        boolean isSelected = theme.equalsIgnoreCase(selectedTheme);
        if (isSelected) {
            holder.card.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, p)));
            holder.layoutCheckBadge.setVisibility(View.VISIBLE);
            
            // Set badge background to primary color dynamically
            holder.layoutCheckBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, p)));
        } else {
            holder.card.setStrokeColor(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            holder.layoutCheckBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            // Bounce scale-down / scale-up animation on selection
            holder.itemView.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(100)
                .withEndAction(() -> {
                    holder.itemView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            if (listener != null) {
                                listener.onThemeClick(theme);
                            }
                        })
                        .start();
                })
                .start();
        });
    }

    @Override
    public int getItemCount() { return themes.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvName;
        View v1, v2, v3, v4;
        View layoutCheckBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_theme_preview);
            tvName = itemView.findViewById(R.id.tv_theme_name);
            v1 = itemView.findViewById(R.id.view_color_1);
            v2 = itemView.findViewById(R.id.view_color_2);
            v3 = itemView.findViewById(R.id.view_color_3);
            v4 = itemView.findViewById(R.id.view_color_4);
            layoutCheckBadge = itemView.findViewById(R.id.layout_check_badge);
        }
    }
}
