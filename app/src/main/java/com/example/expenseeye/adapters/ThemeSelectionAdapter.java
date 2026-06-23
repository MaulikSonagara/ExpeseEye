package com.example.expenseeye.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
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
    private String selectedTheme;
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
        holder.rbSelected.setChecked(theme.equalsIgnoreCase(selectedTheme));

        Context ctx = holder.itemView.getContext();
        int p, s, b;

        if (ThemePreferenceHelper.THEME_FOREST.equalsIgnoreCase(theme)) {
            p = R.color.fl_l_primary; s = R.color.fl_l_secondary; b = R.color.fl_l_background;
        } else if (ThemePreferenceHelper.THEME_SAND.equalsIgnoreCase(theme)) {
            p = R.color.ms_l_primary; s = R.color.ms_l_secondary; b = R.color.ms_l_background;
        } else if (ThemePreferenceHelper.THEME_OCEAN.equalsIgnoreCase(theme)) {
            p = R.color.ol_l_primary; s = R.color.ol_l_secondary; b = R.color.ol_l_background;
        } else {
            p = R.color.mc_l_primary; s = R.color.mc_l_secondary; b = R.color.mc_l_background;
        }

        holder.v1.setBackgroundColor(ContextCompat.getColor(ctx, p));
        holder.v2.setBackgroundColor(ContextCompat.getColor(ctx, s));
        holder.v3.setBackgroundColor(ContextCompat.getColor(ctx, b));

        if (theme.equalsIgnoreCase(selectedTheme)) {
            holder.card.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, p)));
        } else {
            holder.card.setStrokeColor(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onThemeClick(theme);
        });
    }

    @Override
    public int getItemCount() { return themes.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvName;
        View v1, v2, v3;
        RadioButton rbSelected;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_theme_preview);
            tvName = itemView.findViewById(R.id.tv_theme_name);
            v1 = itemView.findViewById(R.id.view_color_1);
            v2 = itemView.findViewById(R.id.view_color_2);
            v3 = itemView.findViewById(R.id.view_color_3);
            rbSelected = itemView.findViewById(R.id.rb_selected);
        }
    }
}
