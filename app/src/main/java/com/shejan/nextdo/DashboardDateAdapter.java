package com.shejan.nextdo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class DashboardDateAdapter extends RecyclerView.Adapter<DashboardDateAdapter.DateViewHolder> {

    private final List<DateItem> dates = Arrays.asList(
            new DateItem("MON", "12", false),
            new DateItem("TUE", "13", false),
            new DateItem("WED", "14", true), // Active
            new DateItem("THU", "15", false),
            new DateItem("FRI", "16", false),
            new DateItem("SAT", "17", false),
            new DateItem("SUN", "18", false));

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_date, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        DateItem item = dates.get(position);
        holder.textDay.setText(item.day);
        holder.textDate.setText(item.date);

        if (item.isActive) {
            holder.itemView.setBackgroundResource(R.drawable.bg_dashboard_date_selected);
            holder.textDay
                    .setTextColor(holder.itemView.getContext().getResources().getColor(R.color.dashboard_text_navy)); // Darker
                                                                                                                      // for
                                                                                                                      // active
            // Remove transparency or make sure it looks "Bold"
        } else {
            holder.itemView.setBackground(null);
            holder.textDay.setTextColor(0xFF2C3E50); // Navy
            holder.textDay.setAlpha(0.4f); // Opacity 40%
        }
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView textDay, textDate;

        DateViewHolder(View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.text_day);
            textDate = itemView.findViewById(R.id.text_date);
        }
    }

    static class DateItem {
        String day;
        String date;
        boolean isActive;

        DateItem(String day, String date, boolean isActive) {
            this.day = day;
            this.date = date;
            this.isActive = isActive;
        }
    }
}
