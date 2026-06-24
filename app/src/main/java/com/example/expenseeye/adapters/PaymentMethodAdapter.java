package com.example.expenseeye.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
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

public class PaymentMethodAdapter extends RecyclerView.Adapter<PaymentMethodAdapter.ViewHolder> {

    public interface OnPaymentMethodClickListener {
        void onPaymentMethodClick(String name);
    }

    private final List<String> items;
    private String selectedMethod;
    private final OnPaymentMethodClickListener listener;

    public PaymentMethodAdapter(List<String> items, String selectedMethod, OnPaymentMethodClickListener listener) {
        this.items = items;
        this.selectedMethod = selectedMethod;
        this.listener = listener;
    }

    public void setSelectedMethod(String selectedMethod) {
        android.util.Log.d("PaymentAdapter", "Selected: " + selectedMethod);
        this.selectedMethod = selectedMethod;
        notifyDataSetChanged();
    }

    public String getSelectedMethod() {
        return selectedMethod;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.payment_option_chip, parent, false);
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

        int iconRes = R.drawable.ic_other;
        if ("Cash".equalsIgnoreCase(name)) iconRes = R.drawable.ic_cash;
        else if ("UPI".equalsIgnoreCase(name)) iconRes = R.drawable.ic_upi;
        else if ("Debit Card".equalsIgnoreCase(name) || "Credit Card".equalsIgnoreCase(name) || "Debit/Credit".equalsIgnoreCase(name)) iconRes = R.drawable.ic_card;
        else if ("Bank Transfer".equalsIgnoreCase(name)) iconRes = R.drawable.ic_bank;

        holder.ivIcon.setImageResource(iconRes);

        boolean isSelected = name.equalsIgnoreCase(selectedMethod);
        android.util.Log.d("PaymentAdapter", "Pos: " + position + ", Name: " + name + ", Selected: " + selectedMethod + ", IsSelected: " + isSelected);

        // Convert 2dp and 4dp to pixels
        float density = ctx.getResources().getDisplayMetrics().density;
        int strokeWidthSelected = Math.round(3 * density);
        int strokeWidthUnselected = Math.round(1.5f * density);

        if (isSelected) {
            holder.cardView.setCardBackgroundColor(ColorStateList.valueOf(elevatedSurface));
            holder.cardView.setStrokeColor(ColorStateList.valueOf(primaryColor));
            holder.cardView.setStrokeWidth(strokeWidthSelected);
            holder.tvLabel.setTextColor(primaryColor);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(primaryColor));
            holder.cardView.setCardElevation(2 * density);
        } else {
            holder.cardView.setCardBackgroundColor(ColorStateList.valueOf(surfaceColor));
            holder.cardView.setStrokeColor(ColorStateList.valueOf(dividerColor));
            holder.cardView.setStrokeWidth(strokeWidthUnselected);
            holder.tvLabel.setTextColor(textSecondary);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(textSecondary));
            holder.cardView.setCardElevation(0);
        }

        holder.itemView.setOnClickListener(v -> {
            setSelectedMethod(name);
            if (listener != null) listener.onPaymentMethodClick(name);
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
