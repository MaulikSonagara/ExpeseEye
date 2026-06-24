package com.example.expenseeye.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;

import java.util.List;

public class IconPickerAdapter extends RecyclerView.Adapter<IconPickerAdapter.IconViewHolder> {

    private final List<String> iconNames;
    private String selectedIconName;
    private int categoryColor;
    private final OnIconSelectedListener listener;

    public interface OnIconSelectedListener {
        void onIconSelected(String iconName);
    }

    public IconPickerAdapter(List<String> iconNames, String initialSelectedIcon, int categoryColor, OnIconSelectedListener listener) {
        this.iconNames = iconNames;
        this.selectedIconName = initialSelectedIcon;
        this.categoryColor = categoryColor;
        this.listener = listener;
    }

    public String getSelectedIconName() {
        return selectedIconName;
    }

    public void setSelectedIconName(String selectedIconName) {
        this.selectedIconName = selectedIconName;
        notifyDataSetChanged();
    }

    public void setCategoryColor(int categoryColor) {
        this.categoryColor = categoryColor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon_picker, parent, false);
        return new IconViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        String iconName = iconNames.get(position);

        int resId = holder.itemView.getContext().getResources().getIdentifier(
                iconName, "drawable", holder.itemView.getContext().getPackageName()
        );
        if (resId != 0) {
            holder.ivIcon.setImageResource(resId);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_other);
        }

        if (iconName.equals(selectedIconName)) {
            holder.viewRing.setVisibility(View.VISIBLE);
            holder.iconCircle.setBackgroundTintList(ColorStateList.valueOf(categoryColor));
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.white)));
        } else {
            holder.viewRing.setVisibility(View.INVISIBLE);
            
            int tintColor = 0xFF888888;
            int bgCircleColor = 0x1A888888;
            android.util.TypedValue typedValue = new android.util.TypedValue();
            if (holder.itemView.getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)) {
                tintColor = typedValue.data;
            }
            if (holder.itemView.getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, typedValue, true)) {
                bgCircleColor = typedValue.data;
            }
            
            holder.iconCircle.setBackgroundTintList(ColorStateList.valueOf(bgCircleColor));
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(tintColor));
        }

        holder.itemView.setOnClickListener(v -> {
            selectedIconName = iconName;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onIconSelected(iconName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return iconNames.size();
    }

    static class IconViewHolder extends RecyclerView.ViewHolder {
        View viewRing;
        FrameLayout iconCircle;
        ImageView ivIcon;

        public IconViewHolder(@NonNull View itemView) {
            super(itemView);
            viewRing = itemView.findViewById(R.id.view_ring);
            iconCircle = itemView.findViewById(R.id.icon_circle);
            ivIcon = itemView.findViewById(R.id.iv_icon);
        }
    }
}
