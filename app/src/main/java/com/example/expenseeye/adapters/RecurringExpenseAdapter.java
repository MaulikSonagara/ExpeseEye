package com.example.expenseeye.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.models.RecurringExpense;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

public class RecurringExpenseAdapter extends ListAdapter<RecurringExpense, RecurringExpenseAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(RecurringExpense re);
    }

    public interface OnToggleListener {
        void onToggle(RecurringExpense re, boolean enabled);
    }

    private final OnItemClickListener clickListener;
    private final OnToggleListener toggleListener;

    public RecurringExpenseAdapter(OnItemClickListener clickListener, OnToggleListener toggleListener) {
        super(new DiffUtil.ItemCallback<RecurringExpense>() {
            @Override
            public boolean areItemsTheSame(@NonNull RecurringExpense oldItem, @NonNull RecurringExpense newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull RecurringExpense oldItem, @NonNull RecurringExpense newItem) {
                return oldItem.getTitle().equals(newItem.getTitle()) &&
                        oldItem.getAmount() == newItem.getAmount() &&
                        oldItem.isEnabled() == newItem.isEnabled() &&
                        oldItem.getFrequency().equals(newItem.getFrequency());
            }
        });
        this.clickListener = clickListener;
        this.toggleListener = toggleListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recurring_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDetails, tvAmount;
        private final MaterialSwitch switchEnabled;
        private final ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDetails = itemView.findViewById(R.id.tv_details);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            switchEnabled = itemView.findViewById(R.id.switch_enabled);
            ivIcon = itemView.findViewById(R.id.iv_category_icon);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) clickListener.onItemClick(getItem(pos));
            });
        }

        public void bind(RecurringExpense re) {
            tvTitle.setText(re.getTitle());
            tvDetails.setText(String.format("%s • %s", re.getFrequency(), re.getPaymentMethodName()));
            tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", re.getAmount()));
            
            // Avoid listener triggering during bind
            switchEnabled.setOnCheckedChangeListener(null);
            switchEnabled.setChecked(re.isEnabled());
            switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> toggleListener.onToggle(re, isChecked));
        }
    }
}
