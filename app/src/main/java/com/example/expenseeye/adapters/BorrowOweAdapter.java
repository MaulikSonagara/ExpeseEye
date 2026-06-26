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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.models.BorrowOwe;
import com.example.expenseeye.theme.ThemeManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BorrowOweAdapter extends ListAdapter<BorrowOwe, BorrowOweAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(BorrowOwe item);
        void onSettleClick(BorrowOwe item);
    }

    private final OnItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final String currencySymbol;

    public BorrowOweAdapter(String currencySymbol, OnItemClickListener listener) {
        super(new DiffUtil.ItemCallback<BorrowOwe>() {
            @Override
            public boolean areItemsTheSame(@NonNull BorrowOwe oldItem, @NonNull BorrowOwe newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull BorrowOwe oldItem, @NonNull BorrowOwe newItem) {
                return oldItem.getPersonName().equals(newItem.getPersonName()) &&
                        oldItem.getAmount() == newItem.getAmount() &&
                        oldItem.isBorrow() == newItem.isBorrow() &&
                        oldItem.isSettled() == newItem.isSettled() &&
                        oldItem.getDueTimestamp() == newItem.getDueTimestamp() &&
                        oldItem.isWasAddedAsExpense() == newItem.isWasAddedAsExpense() &&
                        (oldItem.getDescription() == null ? newItem.getDescription() == null : oldItem.getDescription().equals(newItem.getDescription()));
            }
        });
        this.currencySymbol = currencySymbol;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_borrow_owe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPersonName, tvTypeBadge, tvDescription, tvAmount, tvDueDate;
        private final MaterialButton btnSettle;
        private final ImageView ivSettledBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPersonName = itemView.findViewById(R.id.tv_person_name);
            tvTypeBadge = itemView.findViewById(R.id.tv_type_badge);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            btnSettle = itemView.findViewById(R.id.btn_settle);
            ivSettledBadge = itemView.findViewById(R.id.iv_settled_badge);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(pos));
                }
            });

            btnSettle.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSettleClick(getItem(pos));
                }
            });
        }

        public void bind(BorrowOwe item) {
            Context context = itemView.getContext();
            tvPersonName.setText(item.getPersonName());
            
            // Format Amount
            tvAmount.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, item.getAmount()));

            // Description
            if (item.getDescription() != null && !item.getDescription().trim().isEmpty()) {
                tvDescription.setVisibility(View.VISIBLE);
                tvDescription.setText(item.getDescription());
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Type Badge and Colors
            int dangerColor = ThemeManager.getColor(context, ThemeManager.ThemeColor.DANGER);
            int successColor = ThemeManager.getColor(context, ThemeManager.ThemeColor.SUCCESS);

            if (item.isBorrow()) {
                // I borrowed = I owe them
                tvTypeBadge.setText("OWE");
                tvTypeBadge.setTextColor(dangerColor);
                tvTypeBadge.setBackgroundTintList(ColorStateList.valueOf(adjustAlpha(dangerColor, 0.15f)));
                tvAmount.setTextColor(dangerColor);
            } else {
                // I lent = They owe me
                tvTypeBadge.setText("LENT");
                tvTypeBadge.setTextColor(successColor);
                tvTypeBadge.setBackgroundTintList(ColorStateList.valueOf(adjustAlpha(successColor, 0.15f)));
                tvAmount.setTextColor(successColor);
            }

            // Due Date
            if (item.getDueTimestamp() > 0) {
                tvDueDate.setVisibility(View.VISIBLE);
                String dateStr = dateFormat.format(new Date(item.getDueTimestamp()));
                tvDueDate.setText(String.format("Due: %s", dateStr));

                // Check if overdue
                if (!item.isSettled() && item.getDueTimestamp() < System.currentTimeMillis()) {
                    tvDueDate.setTextColor(dangerColor);
                } else {
                    tvDueDate.setTextColor(ThemeManager.getColor(context, ThemeManager.ThemeColor.TEXT_SECONDARY));
                }
            } else {
                tvDueDate.setVisibility(View.GONE);
            }

            // Settled State Views
            if (item.isSettled()) {
                btnSettle.setVisibility(View.GONE);
                ivSettledBadge.setVisibility(View.VISIBLE);
                tvAmount.setTextColor(ThemeManager.getColor(context, ThemeManager.ThemeColor.TEXT_SECONDARY));
                tvDueDate.setTextColor(ThemeManager.getColor(context, ThemeManager.ThemeColor.TEXT_SECONDARY));
                itemView.setAlpha(0.6f);
            } else {
                btnSettle.setVisibility(View.VISIBLE);
                ivSettledBadge.setVisibility(View.GONE);
                itemView.setAlpha(1.0f);
            }
        }

        private int adjustAlpha(int color, float factor) {
            int alpha = Math.round(Color.alpha(color) * factor);
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            return Color.argb(alpha, red, green, blue);
        }
    }
}
