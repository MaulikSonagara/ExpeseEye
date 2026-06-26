package com.example.expenseeye.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.models.BorrowOwePayment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BorrowOwePaymentAdapter extends ListAdapter<BorrowOwePayment, BorrowOwePaymentAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(BorrowOwePayment payment);
    }

    private final String currencySymbol;
    private final OnDeleteClickListener deleteClickListener;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public BorrowOwePaymentAdapter(String currencySymbol, OnDeleteClickListener deleteClickListener) {
        super(new DiffUtil.ItemCallback<BorrowOwePayment>() {
            @Override
            public boolean areItemsTheSame(@NonNull BorrowOwePayment oldItem, @NonNull BorrowOwePayment newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull BorrowOwePayment oldItem, @NonNull BorrowOwePayment newItem) {
                return oldItem.getAmountPaid() == newItem.getAmountPaid() &&
                        oldItem.getTimestamp() == newItem.getTimestamp() &&
                        (oldItem.getNote() == null ? newItem.getNote() == null : oldItem.getNote().equals(newItem.getNote()));
            }
        });
        this.currencySymbol = currencySymbol;
        this.deleteClickListener = deleteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_borrow_owe_payment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAmount, tvNote, tvDate;
        private final ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tv_payment_amount);
            tvNote = itemView.findViewById(R.id.tv_payment_note);
            tvDate = itemView.findViewById(R.id.tv_payment_date);
            btnDelete = itemView.findViewById(R.id.btn_delete_payment);

            btnDelete.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(getItem(pos));
                }
            });
        }

        public void bind(BorrowOwePayment payment) {
            tvAmount.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, payment.getAmountPaid()));
            
            if (payment.getNote() != null && !payment.getNote().trim().isEmpty()) {
                tvNote.setVisibility(View.VISIBLE);
                tvNote.setText(payment.getNote());
            } else {
                tvNote.setVisibility(View.GONE);
            }

            tvDate.setText(dateTimeFormat.format(new Date(payment.getTimestamp())));
        }
    }
}
