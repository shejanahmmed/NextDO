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
    private final java.util.Set<Integer> expandedPositions = new java.util.HashSet<>();

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

        // Reset any leftover animation properties
        holder.cardContainer.animate().cancel();
        holder.cardContainer.setAlpha(1f);

        // Match hollow circle to task completion state
        if (task.isCompleted) {
            holder.hollowCircle.setVisibility(View.VISIBLE);
            holder.hollowCircle.setAlpha(1f);
            holder.hollowCircle.setTag(true);
        } else {
            holder.hollowCircle.setVisibility(View.GONE);
            holder.hollowCircle.setAlpha(0f);
            holder.hollowCircle.setTag(false);
        }

        holder.textTitle.setText(task.title);

        if (task.reminderTime > 0) {
            holder.textTime.setText(timeFormat.format(new java.util.Date(task.reminderTime)));
        } else {
            holder.textTime.setText("");
        }

        if (task.reminderType != null && !task.reminderType.isEmpty()) {
            holder.textReminderType.setText(task.reminderType);
            holder.textReminderType.setVisibility(View.VISIBLE);
        } else {
            holder.textReminderType.setVisibility(View.GONE);
        }

        // Handle description
        if (task.description != null && !task.description.isEmpty()) {
            holder.textDescription.setText(task.description);
            boolean isExpanded = expandedPositions.contains(position);
            holder.textDescription.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        } else {
            holder.textDescription.setVisibility(View.GONE);
        }

        // Click listener for expand/collapse
        holder.cardContainer.setOnClickListener(v -> {
            if (task.description != null && !task.description.isEmpty()) {
                if (expandedPositions.contains(position)) {
                    // Collapse with animation
                    expandedPositions.remove(position);
                    collapseView(holder.textDescription);
                } else {
                    // Expand with animation
                    expandedPositions.add(position);
                    expandView(holder.textDescription);
                }
            }
        });

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
        holder.hollowCircle.setColorFilter(color);

        if (task.isCompleted) {
            holder.imgCheck.setVisibility(View.VISIBLE);
        } else {
            holder.imgCheck.setVisibility(View.GONE);
        }
    }

    private void expandView(View view) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null);
    }

    private int dpToPx(int dp) {
        return (int) (dp * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }

    private void collapseView(View view) {
        view.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        view.setVisibility(View.GONE);
                        view.setAlpha(1f);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textTime, textReminderType, textDescription;
        ImageView imgCheck;
        LinearLayout cardContainer;
        ImageView hollowCircle;
        View timelineDot;

        TaskViewHolder(View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_task_title);
            textTime = itemView.findViewById(R.id.text_task_time);
            textReminderType = itemView.findViewById(R.id.text_reminder_type);
            textDescription = itemView.findViewById(R.id.text_task_description);
            imgCheck = itemView.findViewById(R.id.img_check);
            cardContainer = itemView.findViewById(R.id.card_container);
            timelineDot = itemView.findViewById(R.id.timeline_dot);
            hollowCircle = itemView.findViewById(R.id.hollow_circle_indicator);
        }
    }
}
