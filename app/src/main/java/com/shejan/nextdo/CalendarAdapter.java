package com.shejan.nextdo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private final List<Calendar> days;
    private final OnDateSelectedListener listener;
    private Calendar selectedDate;
    private final Calendar today;

    public interface OnDateSelectedListener {
        void onDateSelected(Calendar date);
    }

    public CalendarAdapter(List<Calendar> days, Calendar selectedDate, OnDateSelectedListener listener) {
        this.days = days;
        this.selectedDate = selectedDate;
        this.listener = listener;
        this.today = Calendar.getInstance();
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        Calendar date = days.get(position);
        if (date == null) {
            holder.textDay.setText("");
            holder.textDay.setBackground(null);
            holder.itemView.setOnClickListener(null);
        } else {
            holder.textDay.setText(String.valueOf(date.get(Calendar.DAY_OF_MONTH)));

            boolean isSelected = isSameDay(date, selectedDate);
            boolean isToday = isSameDay(date, today);

            holder.itemView.setSelected(isSelected);
            // We use setChecked for "today" state if using a Checkable view or custom
            // state,
            // but here we rely on the selector.
            // The selector uses state_selected for the solid circle.
            // For the hollow circle (today), we can use another state or just set drawable
            // manually if not selected.

            // Let's simplify:
            // Selected -> Solid White Circle (Text Black)
            // Today -> Hollow White Circle (Text White)
            // Normal -> No background (Text White)

            if (isSelected) {
                holder.textDay.setBackgroundResource(R.drawable.bg_calendar_selected);
                holder.textDay.setTextColor(holder.itemView.getContext().getColor(R.color.black));
            } else if (isToday) {
                holder.textDay.setBackgroundResource(R.drawable.bg_calendar_today);
                holder.textDay.setTextColor(holder.itemView.getContext().getColor(R.color.white));
            } else {
                holder.textDay.setBackground(null);
                holder.textDay.setTextColor(holder.itemView.getContext().getColor(R.color.white));
            }

            holder.itemView.setOnClickListener(v -> {
                selectedDate = date;
                // noinspection NotifyDataSetChanged
                notifyDataSetChanged();
                listener.onDateSelected(date);
            });
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null)
            return false;
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public void updateDays(List<Calendar> newDays) {
        days.clear();
        days.addAll(newDays);
        // noinspection NotifyDataSetChanged
        notifyDataSetChanged();
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView textDay;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.text_day);
        }
    }
}
