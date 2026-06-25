package com.example.expenseeye.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenseeye.R;
import com.example.expenseeye.models.Category;
import com.example.expenseeye.models.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseAdapter extends ListAdapter<ExpenseAdapter.ListItem, RecyclerView.ViewHolder> {

    public static class ListItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_EXPENSE = 1;

        public final int type;
        public final String date;
        public final Expense expense;

        public ListItem(String date) {
            this.type = TYPE_HEADER;
            this.date = date;
            this.expense = null;
        }

        public ListItem(Expense expense) {
            this.type = TYPE_EXPENSE;
            this.date = null;
            this.expense = expense;
        }
    }

    private final Map<String, Category> categoryMap = new HashMap<>();
    private final OnExpenseClickListener clickListener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public ExpenseAdapter(OnExpenseClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<ListItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ListItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
            if (oldItem.type != newItem.type) {
                return false;
            }
            if (oldItem.type == ListItem.TYPE_HEADER) {
                return oldItem.date.equals(newItem.date);
            } else {
                return oldItem.expense.getId() == newItem.expense.getId();
            }
        }

        @Override
        public boolean areContentsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
            if (oldItem.type == ListItem.TYPE_HEADER) {
                return oldItem.date.equals(newItem.date);
            } else {
                Expense oldExpense = oldItem.expense;
                Expense newExpense = newItem.expense;
                return oldExpense.getTitle().equals(newExpense.getTitle()) &&
                        oldExpense.getAmount() == newExpense.getAmount() &&
                        oldExpense.getTimestamp() == newExpense.getTimestamp() &&
                        oldExpense.getCategoryName().equals(newExpense.getCategoryName()) &&
                        oldExpense.getPaymentMethodName().equals(newExpense.getPaymentMethodName()) &&
                        (oldExpense.getDescription() == null ? newExpense.getDescription() == null : oldExpense.getDescription().equals(newExpense.getDescription()));
            }
        }
    };

    public void setCategories(List<Category> categories) {
        categoryMap.clear();
        if (categories != null) {
            for (Category category : categories) {
                categoryMap.put(category.getName(), category);
            }
        }
        notifyDataSetChanged();
    }

    public Expense getExpenseAt(int position) {
        ListItem item = getItem(position);
        if (item != null && item.type == ListItem.TYPE_EXPENSE) {
            return item.expense;
        }
        return null;
    }

    public void submitExpenseList(List<Expense> list) {
        if (list == null) {
            super.submitList(null);
            return;
        }
        List<ListItem> items = new ArrayList<>();
        String lastDateKey = null;
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        for (Expense expense : list) {
            String dateKey = dayFormat.format(new Date(expense.getTimestamp()));
            if (lastDateKey == null || !dateKey.equals(lastDateKey)) {
                String friendlyDate = getFriendlyDateString(expense.getTimestamp());
                items.add(new ListItem(friendlyDate));
                lastDateKey = dateKey;
            }
            items.add(new ListItem(expense));
        }
        super.submitList(items);
    }

    private static String getFriendlyDateString(long timestamp) {
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestamp);

        if (today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return "TODAY";
        }

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return "YESTERDAY";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        return sdf.format(target.getTime()).toUpperCase();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ListItem.TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
            return new ExpenseViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = getItem(position);
        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).bind(item.date);
        } else if (holder instanceof ExpenseViewHolder) {
            ((ExpenseViewHolder) holder).bind(item.expense);
        }

        // Slide-up + fade-in list item animation
        holder.itemView.setAlpha(0.0f);
        holder.itemView.setTranslationY(50.0f);
        holder.itemView.animate()
                .alpha(1.0f)
                .translationY(0.0f)
                .setDuration(350)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    public static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText;

        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.tv_date_header);
        }

        public void bind(String date) {
            dateText.setText(date);
        }
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView categoryText;
        private final TextView paymentMethodText;
        private final TextView descriptionText;
        private final TextView amountText;
        private final TextView timeText;
        private final ImageView categoryIcon;
        private final FrameLayout iconContainer;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.expense_title);
            categoryText = itemView.findViewById(R.id.expense_category);
            paymentMethodText = itemView.findViewById(R.id.expense_payment_method);
            descriptionText = itemView.findViewById(R.id.expense_description);
            amountText = itemView.findViewById(R.id.expense_amount);
            timeText = itemView.findViewById(R.id.expense_time);
            categoryIcon = itemView.findViewById(R.id.category_icon);
            iconContainer = itemView.findViewById(R.id.category_icon_container);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    ListItem item = getItem(pos);
                    if (item != null && item.type == ListItem.TYPE_EXPENSE) {
                        clickListener.onExpenseClick(item.expense);
                    }
                }
            });
        }

        public void bind(Expense expense) {
            titleText.setText(expense.getTitle());
            categoryText.setText(expense.getCategoryName());
            paymentMethodText.setText(expense.getPaymentMethodName());
            
            com.example.expenseeye.theme.ThemePreferenceHelper prefHelper = new com.example.expenseeye.theme.ThemePreferenceHelper(itemView.getContext());
            String currency = prefHelper.getCurrencySymbol();
            amountText.setText(String.format(Locale.getDefault(), "- %s%.2f", currency, expense.getAmount()));

            if (expense.getDescription() != null && !expense.getDescription().trim().isEmpty()) {
                descriptionText.setText(expense.getDescription());
                descriptionText.setVisibility(View.VISIBLE);
            } else {
                descriptionText.setVisibility(View.GONE);
            }

            // Display time + date relative style
            long time = expense.getTimestamp();
            Date date = new Date(time);
            timeText.setText(String.format("%s, %s", dateFormat.format(date), timeFormat.format(date)));

            // Resolve color and icon
            Context context = itemView.getContext();
            Category cat = categoryMap.get(expense.getCategoryName());
            int color = Color.parseColor("#9E9E9E"); // Default grey
            String iconName = "ic_other";

            if (cat != null) {
                color = cat.getColor();
                iconName = cat.getIconName();
            }

            // Draw circular background with soft category color (15% alpha)
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            int softBgColor = ColorUtils.setAlphaComponent(color, 38); // ~15% alpha
            drawable.setColor(softBgColor);
            iconContainer.setBackground(drawable);

            int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
            if (resId != 0) {
                categoryIcon.setImageResource(resId);
            } else {
                categoryIcon.setImageResource(R.drawable.ic_other);
            }
            // Tint the icon with the solid category color
            categoryIcon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }
}
