package com.shejan.nextdo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class DashboardTaskAdapter extends RecyclerView.Adapter<DashboardTaskAdapter.TaskViewHolder> {

    private List<Task> tasks = new java.util.ArrayList<>();
    private final java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm a",
            java.util.Locale.getDefault());

    public void setTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.textTitle.setText(task.title);

        if (task.reminderTime > 0) {
            holder.textTime.setText(timeFormat.format(new java.util.Date(task.reminderTime)));
        } else {
            holder.textTime.setText("");
        }

        // Rotate Card Backgrounds based on position
        int bgResId;
        int color;
        int mod = position % 3;
        if (mod == 0) {
            bgResId = R.drawable.bg_dashboard_card_blue;
            color = 0xFFA2D2FF;
        } else if (mod == 1) {
            bgResId = R.drawable.bg_dashboard_card_pink;
            color = 0xFFFF9AA2;
        } else {
            bgResId = R.drawable.bg_dashboard_card_orange; // Assuming orange exists or fallback
            color = 0xFFFFDAB9;
        }

        // Ensure orange resource exists, otherwise fallback to blue
        try {
            holder.cardContainer.setBackgroundResource(bgResId);
        } catch (Exception e) {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_dashboard_card_blue);
            color = 0xFFA2D2FF;
        }

        holder.timelineDot.setBackgroundResource(R.drawable.circle_black);
        holder.timelineDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

        if (task.isCompleted) {
            holder.textTitle
                    .setPaintFlags(holder.textTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            holder.imgCheck.setVisibility(View.VISIBLE);
        } else {
            holder.textTitle
                    .setPaintFlags(holder.textTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            holder.imgCheck.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textTime;
        ImageView imgCheck;
        LinearLayout cardContainer;
        View timelineDot;

        TaskViewHolder(View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_task_title);
            textTime = itemView.findViewById(R.id.text_task_time);
            imgCheck = itemView.findViewById(R.id.img_check);
            cardContainer = itemView.findViewById(R.id.card_container);
            timelineDot = itemView.findViewById(R.id.timeline_dot);
        }
    }
}
