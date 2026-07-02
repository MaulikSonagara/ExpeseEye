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
import com.example.expenseeye.models.Trip;
import com.example.expenseeye.theme.ThemePreferenceHelper;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TripAdapter extends ListAdapter<Trip, TripAdapter.ViewHolder> {

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
        void onTripLongClick(Trip trip);
    }

    private final OnTripClickListener listener;
    private final Map<Integer, Double> tripSpentMap = new HashMap<>();

    public TripAdapter(OnTripClickListener listener) {
        super(new DiffUtil.ItemCallback<Trip>() {
            @Override
            public boolean areItemsTheSame(@NonNull Trip oldItem, @NonNull Trip newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Trip oldItem, @NonNull Trip newItem) {
                return oldItem.getTitle().equals(newItem.getTitle()) &&
                        oldItem.getBudget() == newItem.getBudget() &&
                        oldItem.isActive() == newItem.isActive() &&
                        oldItem.getStartTimestamp() == newItem.getStartTimestamp() &&
                        oldItem.getEndTimestamp() == newItem.getEndTimestamp();
            }
        });
        this.listener = listener;
    }

    public void setTripSpent(int tripId, double spent) {
        tripSpentMap.put(tripId, spent);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDates, tvSpent, tvBudget;
        private final View chipActive;
        private final LinearProgressIndicator progressTrip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_trip_title);
            tvDates = itemView.findViewById(R.id.tv_trip_dates);
            tvSpent = itemView.findViewById(R.id.tv_trip_spent);
            tvBudget = itemView.findViewById(R.id.tv_trip_budget);
            chipActive = itemView.findViewById(R.id.chip_active);
            progressTrip = itemView.findViewById(R.id.progress_trip);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onTripClick(getItem(pos));
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onTripLongClick(getItem(pos));
                    return true;
                }
                return false;
            });
        }

        public void bind(Trip trip) {
            tvTitle.setText(trip.getTitle());
            chipActive.setVisibility(trip.isActive() ? View.VISIBLE : View.GONE);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
            SimpleDateFormat sdfYear = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String dateRange = sdf.format(new Date(trip.getStartTimestamp())) + " - " + sdfYear.format(new Date(trip.getEndTimestamp()));
            tvDates.setText(dateRange);

            ThemePreferenceHelper ph = new ThemePreferenceHelper(itemView.getContext());
            String currency = ph.getCurrencySymbol();

            double spent = tripSpentMap.getOrDefault(trip.getId(), 0.0);
            tvSpent.setText(String.format(Locale.getDefault(), "%s%.2f", currency, spent));
            tvBudget.setText(String.format(Locale.getDefault(), "%s%.2f", currency, trip.getBudget()));

            if (trip.getBudget() > 0) {
                int progress = (int) ((spent / trip.getBudget()) * 100);
                progressTrip.setProgress(Math.min(progress, 100));
            } else {
                progressTrip.setProgress(0);
            }
        }
    }
}
