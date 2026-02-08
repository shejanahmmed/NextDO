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

    private final List<TaskItem> tasks = Arrays.asList(
            new TaskItem("Project Kickoff Meeting", "10:00 AM", R.drawable.bg_dashboard_card_blue, false),
            new TaskItem("Project Kickoff Meeting", "10:00 AM", R.drawable.bg_dashboard_card_pink, false),
            new TaskItem("Check Emails", "09:00 AM", R.drawable.bg_dashboard_card_orange, true), // Completed
            new TaskItem("Project Kickoff Meeting", "10:00 AM", R.drawable.bg_dashboard_card_blue, false));

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskItem item = tasks.get(position);

        holder.textTitle.setText(item.title);
        holder.textTime.setText(item.time);
        holder.cardContainer.setBackgroundResource(item.bgResId);

        // Update dot color to match card (or keep generic blue dot as per layout?
        // HTML has different dot colors: #D1E4FF, Matte Blue, Gray, Matte Pink)
        // Let's match the dot to the card usage or specific colors.
        // For simplicity, let's set dot color based on the card background but standard
        // dot drawable is blue.
        // I'll create a generic circle drawable and set color filter.

        int color;
        if (item.bgResId == R.drawable.bg_dashboard_card_blue)
            color = 0xFFA2D2FF;
        else if (item.bgResId == R.drawable.bg_dashboard_card_pink)
            color = 0xFFFF9AA2;
        else
            color = 0xFFFFDAB9;

        // Actually, let's just use the card background drawable for the dot too, but
        // rounded 16dp is a circle for 16x16 view.
        // Or better, set tint.
        holder.timelineDot.setBackgroundResource(R.drawable.circle_black); // Reusing existing circle or create new?
        // Let's use bg_dashboard_card_blue structure but override tint
        holder.timelineDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

        if (item.isCompleted) {
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

    static class TaskItem {
        String title;
        String time;
        int bgResId;
        boolean isCompleted;

        TaskItem(String title, String time, int bgResId, boolean isCompleted) {
            this.title = title;
            this.time = time;
            this.bgResId = bgResId;
            this.isCompleted = isCompleted;
        }
    }
}
