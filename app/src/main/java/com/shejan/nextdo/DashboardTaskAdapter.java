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
import java.util.List;

public class DashboardTaskAdapter extends RecyclerView.Adapter<DashboardTaskAdapter.TaskViewHolder> {

    private List<Task> tasks = new java.util.ArrayList<>();
    private final java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm a",
            java.util.Locale.getDefault());
    private int expandedPosition = RecyclerView.NO_POSITION;

    public interface OnTaskActionListener {
        void onTaskChecked(Task task, boolean isChecked);

        void onTaskEdit(Task task);

        void onTaskDelete(Task task);
    }

    private final OnTaskActionListener actionListener;
    private boolean showTimeline = true;

    public DashboardTaskAdapter(OnTaskActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setShowTimeline(boolean showTimeline) {
        this.showTimeline = showTimeline;
    }

    public void setTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        this.expandedPosition = RecyclerView.NO_POSITION;
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

        // Show/Hide Timeline based on flag
        if (holder.timelineLine != null) {
            holder.timelineLine.setVisibility(showTimeline ? View.VISIBLE : View.GONE);
        }
        if (holder.timelineDot != null) {
            holder.timelineDot.setVisibility(showTimeline ? View.VISIBLE : View.GONE);
        }

        // Adjust card margin if timeline is hidden
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.cardContainer.getLayoutParams();
        if (!showTimeline) {
            params.setMarginStart(0);
        } else {
            params.setMarginStart(dpToPx(24));
        }
        holder.cardContainer.setLayoutParams(params);

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

        // Handle Expandable Layout (Accordion Style)
        boolean isExpanded = position == expandedPosition;

        // Ensure "Edit" and "Mark as Done" buttons are visible within the expandable
        // layout
        holder.layoutExpandable.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.layoutExpandable.setAlpha(isExpanded ? 1f : 0f);

        // Show/hide delete button with expand state
        holder.btnDelete.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.btnDelete.setAlpha(isExpanded ? 0.6f : 0f);

        // Handle description text specifically
        if (task.description != null && !task.description.isEmpty()) {
            holder.textDescription.setText(task.description);
            holder.textDescription.setVisibility(View.VISIBLE);
        } else {
            holder.textDescription.setVisibility(View.GONE);
        }

        // Click listener for expand/collapse on the ENTIRE card
        holder.cardContainer.setOnClickListener(v -> {
            int previousExpandedPosition = expandedPosition;
            if (expandedPosition == position) {
                // Clicked the already expanded card -> Collapse it
                expandedPosition = RecyclerView.NO_POSITION;
                collapseView(holder.layoutExpandable);
            } else {
                // Clicked a different card -> Expand it
                expandedPosition = position;
                expandView(holder.layoutExpandable);

                // Collapse the previously expanded card if it exists
                if (previousExpandedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousExpandedPosition);
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

        // Apply 80% transparency (20% opacity) in Dark Mode
        int nightModeFlags = holder.cardContainer.getContext().getResources().getConfiguration().uiMode & 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            if (holder.cardContainer.getBackground() != null) {
                holder.cardContainer.getBackground().mutate().setAlpha(51); // 20% of 255 is ~51
            }
        } else {
            // Ensure full opacity in Light Mode (alpha 255)
            if (holder.cardContainer.getBackground() != null) {
                holder.cardContainer.getBackground().mutate().setAlpha(255);
            }
        }

        holder.timelineDot.setBackgroundResource(R.drawable.circle_black);
        holder.timelineDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        holder.hollowCircle.setColorFilter(color);

        if (task.isCompleted) {
            holder.imgCheck.setVisibility(View.VISIBLE);
        } else {
            holder.imgCheck.setVisibility(View.GONE);
        }

        // Click listeners for task completion
        View.OnClickListener completionClickListener = v -> {
            boolean newStatus = !task.isCompleted;
            // Optimistic update
            task.isCompleted = newStatus;
            notifyItemChanged(position);

            if (actionListener != null) {
                actionListener.onTaskChecked(task, newStatus);
            }
        };

        holder.hollowCircle.setOnClickListener(completionClickListener);
        // holder.imgCheck is no longer clickable as per user request

        // Buttons
        holder.btnEdit.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onTaskEdit(task);
            }
        });

        // Set adaptive style using resources
        int btnBgColor = androidx.core.content.ContextCompat.getColor(holder.btnDone.getContext(), R.color.task_card_btn_done_bg);
        int btnTextColor = androidx.core.content.ContextCompat.getColor(holder.btnDone.getContext(), R.color.task_card_btn_done_text);
        holder.btnDone.setBackgroundTintList(android.content.res.ColorStateList.valueOf(btnBgColor));
        holder.btnDone.setTextColor(btnTextColor);

        // Toggle text and logic based on completion
        holder.btnDone.setText(task.isCompleted ? "Undone" : "Done");

        holder.btnDone.setOnClickListener(v -> {
            boolean newStatus = !task.isCompleted;
            task.isCompleted = newStatus;
            notifyItemChanged(position);
            if (actionListener != null) {
                actionListener.onTaskChecked(task, newStatus);
            }
        });

        // Delete button (inline)
        if (holder.btnDeleteBtn != null) {
            holder.btnDeleteBtn.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onTaskDelete(task);
                }
            });
        }

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onTaskDelete(task);
            }
        });
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
        ImageView imgCheck, btnDelete;
        android.widget.RelativeLayout cardContainer;
        ImageView hollowCircle;
        LinearLayout layoutExpandable;
        com.google.android.material.button.MaterialButton btnEdit, btnDone, btnDeleteBtn;

        View timelineDot, timelineLine;

        TaskViewHolder(View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_task_title);
            textTime = itemView.findViewById(R.id.text_task_time);
            textReminderType = itemView.findViewById(R.id.text_reminder_type);
            textDescription = itemView.findViewById(R.id.text_task_description);
            imgCheck = itemView.findViewById(R.id.img_check);
            cardContainer = itemView.findViewById(R.id.card_container);
            timelineDot = itemView.findViewById(R.id.timeline_dot);
            timelineLine = itemView.findViewById(R.id.timeline_line);
            hollowCircle = itemView.findViewById(R.id.hollow_circle_indicator);

            layoutExpandable = itemView.findViewById(R.id.layout_expandable);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDone = itemView.findViewById(R.id.btn_done);
            btnDelete = itemView.findViewById(R.id.btn_delete_task);
            btnDeleteBtn = itemView.findViewById(R.id.btn_delete);
        }
    }
}
