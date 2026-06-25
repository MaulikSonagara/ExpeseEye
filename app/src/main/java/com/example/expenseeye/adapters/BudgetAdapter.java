package com.example.expenseeye.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.models.Budget;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.theme.ThemePreferenceHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetAdapter extends ListAdapter<Budget, BudgetAdapter.ViewHolder> {

    public interface OnBudgetClickListener {
        void onBudgetClick(Budget budget);
    }

    private final OnBudgetClickListener listener;
    private final Map<String, String> categoryIconMap = new HashMap<>();

    public BudgetAdapter(OnBudgetClickListener listener) {
        super(new DiffUtil.ItemCallback<Budget>() {
            @Override
            public boolean areItemsTheSame(@NonNull Budget oldItem, @NonNull Budget newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Budget oldItem, @NonNull Budget newItem) {
                return oldItem.getAmount() == newItem.getAmount() &&
                        oldItem.getCategoryName().equals(newItem.getCategoryName()) &&
                        oldItem.getMonth().equals(newItem.getMonth());
            }
        });
        this.listener = listener;
    }

    public void setCategories(List<Category> categories) {
        categoryIconMap.clear();
        if (categories != null) {
            for (Category c : categories) {
                categoryIconMap.put(c.getName(), c.getIconName());
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final android.widget.ImageView ivIcon;
        private final TextView tvCategory, tvMonth, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_budget_icon);
            tvCategory = itemView.findViewById(R.id.tv_budget_category);
            tvMonth = itemView.findViewById(R.id.tv_budget_month);
            tvAmount = itemView.findViewById(R.id.tv_budget_amount);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onBudgetClick(getItem(pos));
            });
        }

        public void bind(Budget budget) {
            tvCategory.setText(budget.getCategoryName());
            
            String iconName = categoryIconMap.get(budget.getCategoryName());
            if (iconName == null || iconName.isEmpty()) iconName = "ic_dashboard";
            int resId = itemView.getContext().getResources().getIdentifier(iconName, "drawable", itemView.getContext().getPackageName());
            if (resId != 0) ivIcon.setImageResource(resId);
            else ivIcon.setImageResource(R.drawable.ic_dashboard);

            // Format month string (MM-yyyy) to readable (June 2026)
            try {
                SimpleDateFormat in = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
                SimpleDateFormat out = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                Date date = in.parse(budget.getMonth());
                if (date != null) tvMonth.setText(out.format(date));
                else tvMonth.setText(budget.getMonth());
            } catch (Exception e) {
                tvMonth.setText(budget.getMonth());
            }

            ThemePreferenceHelper ph = new ThemePreferenceHelper(itemView.getContext());
            String currency = ph.getCurrencySymbol();
            tvAmount.setText(String.format(Locale.getDefault(), "%s%.0f", currency, budget.getAmount()));
        }
    }
}
