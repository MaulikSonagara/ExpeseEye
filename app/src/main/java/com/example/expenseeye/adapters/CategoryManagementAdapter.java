package com.example.expenseeye.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.CategoryKeyword;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class CategoryManagementAdapter extends RecyclerView.Adapter<CategoryManagementAdapter.CategoryViewHolder> {

    private List<Category> categories = new ArrayList<>();
    private List<CategoryKeyword> keywords = new ArrayList<>();
    private final OnCategoryClickListener clickListener;
    private final OnCategoryStatusChangeListener statusListener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public interface OnCategoryStatusChangeListener {
        void onStatusChange(Category category, boolean isEnabled);
    }

    public CategoryManagementAdapter(OnCategoryClickListener clickListener, OnCategoryStatusChangeListener statusListener) {
        this.clickListener = clickListener;
        this.statusListener = statusListener;
    }

    public void setData(List<Category> categories, List<CategoryKeyword> keywords) {
        this.categories = categories != null ? categories : new ArrayList<>();
        this.keywords = keywords != null ? keywords : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_manage, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);

        // Name
        holder.tvCategoryName.setText(category.getName());

        // Status switch (temporarily nullify listener to prevent callback loop during binding)
        holder.switchEnabled.setOnCheckedChangeListener(null);
        holder.switchEnabled.setChecked(category.isEnabled());
        holder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (statusListener != null) {
                statusListener.onStatusChange(category, isChecked);
            }
        });

        // Set category color dot background
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(category.getColor());
        holder.iconContainer.setBackground(shape);

        // Load Icon dynamically
        int resId = holder.itemView.getContext().getResources().getIdentifier(
                category.getIconName(), "drawable", holder.itemView.getContext().getPackageName()
        );
        if (resId != 0) {
            holder.ivCategoryIcon.setImageResource(resId);
        } else {
            holder.ivCategoryIcon.setImageResource(R.drawable.ic_other);
        }

        // Type Badge
        if (category.isDefault()) {
            holder.tvTypeBadge.setText("System");
            holder.tvTypeBadge.setTextColor(holder.itemView.getContext().getColor(R.color.white));
            holder.tvTypeBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(category.getColor()));
        } else {
            holder.tvTypeBadge.setText("Custom");
            holder.tvTypeBadge.setTextColor(holder.itemView.getContext().getColor(R.color.white));
            holder.tvTypeBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3F51B5)); // Indigo for custom
        }

        // Keywords count & summary
        int count = 0;
        StringBuilder kwSummary = new StringBuilder();
        for (CategoryKeyword kw : keywords) {
            if (kw.getCategoryId() == category.getId()) {
                count++;
                if (kwSummary.length() < 30) {
                    if (kwSummary.length() > 0) kwSummary.append(", ");
                    kwSummary.append(kw.getKeyword());
                }
            }
        }

        if (count == 0) {
            holder.tvKeywordCount.setText("No keywords");
        } else {
            String summaryText = kwSummary.toString();
            if (summaryText.length() >= 30) {
                summaryText += "...";
            }
            holder.tvKeywordCount.setText(count + " keyword" + (count > 1 ? "s" : "") + " (" + summaryText + ")");
        }

        // Click actions
        holder.cardCategory.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardCategory;
        FrameLayout iconContainer;
        ImageView ivCategoryIcon;
        TextView tvCategoryName;
        TextView tvTypeBadge;
        TextView tvKeywordCount;
        MaterialSwitch switchEnabled;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCategory = itemView.findViewById(R.id.card_category);
            iconContainer = itemView.findViewById(R.id.icon_container);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvTypeBadge = itemView.findViewById(R.id.tv_type_badge);
            tvKeywordCount = itemView.findViewById(R.id.tv_keyword_count);
            switchEnabled = itemView.findViewById(R.id.switch_enabled);
        }
    }
}
