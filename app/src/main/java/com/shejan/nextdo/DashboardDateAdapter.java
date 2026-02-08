package com.shejan.nextdo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Added import for ImageView
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class DashboardDateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ARROW_LEFT = 0;
    private static final int VIEW_TYPE_DATE = 1;
    private static final int VIEW_TYPE_ARROW_RIGHT = 2;

    private List<DateItem> dates = new java.util.ArrayList<>();
    private OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClicked(DateItem item, int position);
    }

    public void setOnDateClickListener(OnDateClickListener listener) {
        this.listener = listener;
    }

    public void setDates(List<DateItem> newDates) {
        this.dates = newDates;
        notifyDataSetChanged();
    }

    public List<DateItem> getDates() {
        return dates;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_ARROW_LEFT;
        } else if (position == dates.size() + 1) {
            return VIEW_TYPE_ARROW_RIGHT;
        } else {
            return VIEW_TYPE_DATE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ARROW_LEFT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_arrow, parent, false);
            return new ArrowViewHolder(view);
        } else if (viewType == VIEW_TYPE_ARROW_RIGHT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_arrow_right, parent,
                    false);
            return new ArrowViewHolder(view); // Reusing ArrowViewHolder as structures are similar enough (just an
                                              // image)
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_date, parent, false);
            return new DateViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ArrowViewHolder) {
            // Arrow logic (currently non-clickable)
            return;
        }

        // Adjust position for DateItems (index 1 in adapter maps to index 0 in list)
        int dateIndex = position - 1;

        // Safety check
        if (dateIndex < 0 || dateIndex >= dates.size())
            return;

        DateItem item = dates.get(dateIndex);
        DateViewHolder dateHolder = (DateViewHolder) holder;

        dateHolder.textDay.setText(item.dayOfWeek);
        dateHolder.textDate.setText(item.dayOfMonth);

        if (item.isActive) {
            dateHolder.itemView.setBackgroundResource(R.drawable.bg_dashboard_date_selected);
            dateHolder.textDay
                    .setTextColor(
                            dateHolder.itemView.getContext().getResources().getColor(R.color.dashboard_text_navy));
            dateHolder.textDay.setAlpha(1.0f);
            dateHolder.textDate.setAlpha(1.0f);
        } else {
            dateHolder.itemView.setBackground(null);
            dateHolder.textDay.setTextColor(0xFF2C3E50); // Navy
            dateHolder.textDay.setAlpha(0.6f);
            dateHolder.textDate.setAlpha(0.6f);
        }

        // Show/Hide Today Indicator
        if (item.isToday) {
            dateHolder.indicatorDot.setVisibility(View.VISIBLE);
        } else {
            dateHolder.indicatorDot.setVisibility(View.GONE);
        }

        dateHolder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDateClicked(item, dateIndex);

                // Update selection state internally for immediate UI feedback
                for (DateItem d : dates) {
                    d.isActive = false;
                }
                item.isActive = true;
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        // Add 2 for Arrows (Start and End)
        return dates.size() + 2;
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView textDay, textDate;
        View indicatorDot;

        DateViewHolder(View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.text_day);
            textDate = itemView.findViewById(R.id.text_date);
            indicatorDot = itemView.findViewById(R.id.indicator_dot);
        }
    }

    static class ArrowViewHolder extends RecyclerView.ViewHolder { // Added new ViewHolder
        ImageView imgArrow;

        ArrowViewHolder(View itemView) {
            super(itemView);
            imgArrow = itemView.findViewById(R.id.img_arrow);
        }
    }

    public static class DateItem {
        public String dayOfWeek;
        public String dayOfMonth;
        public long timestamp; // Helps with improved filtering
        public boolean isActive;
        public boolean isToday;

        public DateItem(String dayOfWeek, String dayOfMonth, long timestamp, boolean isActive, boolean isToday) {
            this.dayOfWeek = dayOfWeek;
            this.dayOfMonth = dayOfMonth;
            this.timestamp = timestamp;
            this.isActive = isActive;
            this.isToday = isToday;
        }
    }
}
