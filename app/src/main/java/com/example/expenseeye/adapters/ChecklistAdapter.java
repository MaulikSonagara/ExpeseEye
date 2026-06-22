package com.example.expenseeye.adapters;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.models.ChecklistItem;

public class ChecklistAdapter extends ListAdapter<ChecklistItem, ChecklistAdapter.ChecklistViewHolder> {

    private final OnChecklistItemClickListener listener;
    private boolean shoppingMode = false;

    public interface OnChecklistItemClickListener {
        void onCheckChanged(ChecklistItem item, boolean isChecked);
        void onDeleteClick(ChecklistItem item);
    }

    public ChecklistAdapter(OnChecklistItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setShoppingMode(boolean shoppingMode) {
        this.shoppingMode = shoppingMode;
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<ChecklistItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChecklistItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChecklistItem oldItem, @NonNull ChecklistItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChecklistItem oldItem, @NonNull ChecklistItem newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getCategory().equals(newItem.getCategory()) &&
                    oldItem.getPriority().equals(newItem.getPriority()) &&
                    oldItem.isCompleted() == newItem.isCompleted() &&
                    (oldItem.getQuantity() == null ? newItem.getQuantity() == null : oldItem.getQuantity().equals(newItem.getQuantity()));
        }
    };

    @NonNull
    @Override
    public ChecklistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checklist, parent, false);
        return new ChecklistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChecklistViewHolder holder, int position) {
        holder.bind(getItem(position));

        // Slide-up + fade-in list item animation
        holder.itemView.setAlpha(0.0f);
        holder.itemView.setTranslationY(40.0f);
        holder.itemView.animate()
                .alpha(1.0f)
                .translationY(0.0f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    class ChecklistViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBox;
        private final TextView titleText;
        private final TextView categoryText;
        private final TextView priorityText;
        private final TextView quantityText;
        private final ImageView deleteButton;

        public ChecklistViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checklist_checkbox);
            titleText = itemView.findViewById(R.id.checklist_title);
            categoryText = itemView.findViewById(R.id.checklist_category);
            priorityText = itemView.findViewById(R.id.checklist_priority);
            quantityText = itemView.findViewById(R.id.checklist_quantity);
            deleteButton = itemView.findViewById(R.id.checklist_delete);
        }

        public void bind(ChecklistItem item) {
            titleText.setText(item.getTitle());
            categoryText.setText(item.getCategory());

            if (item.getQuantity() != null && !item.getQuantity().trim().isEmpty()) {
                quantityText.setText(item.getQuantity());
                quantityText.setVisibility(View.VISIBLE);
            } else {
                quantityText.setVisibility(View.GONE);
            }

            if (shoppingMode) {
                categoryText.setVisibility(View.GONE);
                priorityText.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
                titleText.setTextSize(20f);
                quantityText.setTextSize(16f);
            } else {
                categoryText.setVisibility(View.VISIBLE);
                priorityText.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);
                titleText.setTextSize(16f);
                quantityText.setTextSize(12f);
            }

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(item.isCompleted());

            if (item.isCompleted()) {
                titleText.setPaintFlags(titleText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                android.util.TypedValue typedValue = new android.util.TypedValue();
                int secondaryColor;
                if (itemView.getContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)) {
                    secondaryColor = typedValue.data;
                } else {
                    secondaryColor = Color.parseColor("#757575");
                }
                titleText.setTextColor(secondaryColor);
            } else {
                titleText.setPaintFlags(titleText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                android.util.TypedValue typedValue = new android.util.TypedValue();
                int primaryColor;
                if (itemView.getContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)) {
                    primaryColor = typedValue.data;
                } else {
                    primaryColor = Color.parseColor("#212121");
                }
                titleText.setTextColor(primaryColor);
            }

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onCheckChanged(item, isChecked);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(item);
                }
            });

            String priority = item.getPriority() != null ? item.getPriority() : "LOW";
            priorityText.setText(priority);

            int bgColor;
            int textColor;

            switch (priority) {
                case "HIGH":
                    bgColor = Color.parseColor("#FDE8E8");
                    textColor = Color.parseColor("#E53E3E");
                    break;
                case "MEDIUM":
                    bgColor = Color.parseColor("#FEF3C7");
                    textColor = Color.parseColor("#D97706");
                    break;
                case "LOW":
                default:
                    bgColor = Color.parseColor("#E1F5FE");
                    textColor = Color.parseColor("#0288D1");
                    break;
            }

            GradientDrawable priorityBg = new GradientDrawable();
            priorityBg.setShape(GradientDrawable.RECTANGLE);
            priorityBg.setCornerRadius(16);
            priorityBg.setColor(bgColor);
            priorityText.setBackground(priorityBg);
            priorityText.setTextColor(textColor);
        }
    }
}
