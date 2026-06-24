package com.example.expenseeye.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;

import java.util.List;

public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder> {

    private final List<Integer> colors;
    private int selectedColor;
    private final OnColorSelectedListener listener;

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    public ColorPickerAdapter(List<Integer> colors, int initialSelectedColor, OnColorSelectedListener listener) {
        this.colors = colors;
        this.selectedColor = initialSelectedColor;
        this.listener = listener;
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(int selectedColor) {
        this.selectedColor = selectedColor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_picker, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        int color = colors.get(position);

        holder.viewColor.setBackgroundTintList(ColorStateList.valueOf(color));

        if (color == selectedColor) {
            holder.viewRing.setVisibility(View.VISIBLE);
        } else {
            holder.viewRing.setVisibility(View.INVISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            selectedColor = color;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onColorSelected(color);
            }
        });
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        View viewRing;
        View viewColor;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            viewRing = itemView.findViewById(R.id.view_ring);
            viewColor = itemView.findViewById(R.id.view_color);
        }
    }
}
