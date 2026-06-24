package com.example.expenseeye.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expenseeye.R;
import com.example.expenseeye.theme.ThemeManager;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class PrioritySelectionAdapter extends RecyclerView.Adapter<PrioritySelectionAdapter.ViewHolder> {

    public interface OnPriorityClickListener {
        void onPriorityClick(String priority);
    }

    private final List<String> items;
    private String selectedPriority;
    private final OnPriorityClickListener listener;

    public PrioritySelectionAdapter(List<String> items, String selectedPriority, OnPriorityClickListener listener) {
        this.items = items;
        this.selectedPriority = selectedPriority;
        this.listener = listener;
    }

    public void setSelectedPriority(String selectedPriority) {
        this.selectedPriority = selectedPriority;
        notifyDataSetChanged();
    }

    public String getSelectedPriority() {
        return selectedPriority;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.priority_option_chip, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = items.get(position);
        holder.tvLabel.setText(name);

        Context ctx = holder.itemView.getContext();
        int primaryColor = ThemeManager.getColor(ctx, ThemeManager.ThemeColor.PRIMARY);
        int textSecondary = ThemeManager.getColor(ctx, ThemeManager.ThemeColor.TEXT_SECONDARY);
        int dividerColor = ThemeManager.getColor(ctx, ThemeManager.ThemeColor.DIVIDER);
        int surfaceColor = ThemeManager.getColor(ctx, ThemeManager.ThemeColor.SURFACE);
        int elevatedSurface = ThemeManager.getColor(ctx, ThemeManager.ThemeColor.ELEVATED_SURFACE);

        holder.ivIcon.setImageResource(R.drawable.ic_priority_dot);

        // Customize the dot color based on priority
        int dotColor;
        switch (name.toUpperCase()) {
            case "HIGH":
                dotColor = Color.parseColor("#E53E3E");
                break;
            case "MEDIUM":
                dotColor = Color.parseColor("#D97706");
                break;
            case "LOW":
            default:
                dotColor = Color.parseColor("#0288D1");
                break;
        }

        boolean isSelected = name.equalsIgnoreCase(selectedPriority);

        float density = ctx.getResources().getDisplayMetrics().density;
        int strokeWidthSelected = Math.round(3 * density);
        int strokeWidthUnselected = Math.round(1.5f * density);

        if (isSelected) {
            holder.cardView.setCardBackgroundColor(ColorStateList.valueOf(elevatedSurface));
            holder.cardView.setStrokeColor(ColorStateList.valueOf(primaryColor));
            holder.cardView.setStrokeWidth(strokeWidthSelected);
            holder.tvLabel.setTextColor(primaryColor);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(dotColor));
            holder.cardView.setCardElevation(2 * density);
        } else {
            holder.cardView.setCardBackgroundColor(ColorStateList.valueOf(surfaceColor));
            holder.cardView.setStrokeColor(ColorStateList.valueOf(dividerColor));
            holder.cardView.setStrokeWidth(strokeWidthUnselected);
            holder.tvLabel.setTextColor(textSecondary);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(dotColor));
            holder.cardView.setCardElevation(0);
        }

        holder.itemView.setOnClickListener(v -> {
            setSelectedPriority(name);
            if (listener != null) listener.onPriorityClick(name);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivIcon;
        TextView tvLabel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_payment_chip);
            ivIcon = itemView.findViewById(R.id.iv_payment_icon);
            tvLabel = itemView.findViewById(R.id.tv_payment_label);
        }
    }
}
