package com.shejan.nextdo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.shejan.nextdo.databinding.ItemTaskNothingBinding;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class TaskListAdapter extends ListAdapter<Task, TaskListAdapter.TaskViewHolder> {

    private final OnTaskInteractionListener listener;

    public interface OnTaskInteractionListener {
        void onTaskCompleted(Task task, boolean isCompleted);

        void onTaskClicked(Task task);

        void onTaskLongClicked(Task task);
    }

    public TaskListAdapter(@NonNull DiffUtil.ItemCallback<Task> diffCallback, OnTaskInteractionListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTaskNothingBinding binding = ItemTaskNothingBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task current = getItem(position);
        holder.bind(current, listener);
    }

    public Task getTaskAt(int position) {
        return getItem(position);
    }

    public static class TaskDiff extends DiffUtil.ItemCallback<Task> {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return Objects.equals(oldItem.title, newItem.title) &&
                    Objects.equals(oldItem.description, newItem.description) &&
                    Objects.equals(oldItem.priority, newItem.priority) &&
                    oldItem.reminderTime == newItem.reminderTime &&
                    Objects.equals(oldItem.repeat, newItem.repeat) &&
                    oldItem.isCompleted == newItem.isCompleted;
        }
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemTaskNothingBinding binding;

        private TaskViewHolder(ItemTaskNothingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final Task task, final OnTaskInteractionListener listener) {
            if (task == null)
                return;

            // Title
            binding.textTitle.setText(task.title != null ? task.title : "");

            // Strike through if completed
            if (task.isCompleted) {
                binding.textTitle.setPaintFlags(
                        binding.textTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                binding.textTitle.setAlpha(0.5f);
            } else {
                binding.textTitle.setPaintFlags(
                        binding.textTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                binding.textTitle.setAlpha(1.0f);
            }

            // Priority Icon
            boolean hasPriority = task.priority != null && !task.priority.isEmpty()
                    && !task.priority.equalsIgnoreCase("NONE");
            if (hasPriority) {
                binding.iconPriority.setVisibility(View.VISIBLE);
                // Set color based on priority
                if (task.priority.equalsIgnoreCase("HIGH")) {
                    binding.iconPriority.setColorFilter(0xFFFF3B30); // Red
                } else if (task.priority.equalsIgnoreCase("MEDIUM")) {
                    binding.iconPriority.setColorFilter(0xFFFFCC00); // Yellow
                } else {
                    binding.iconPriority.setColorFilter(0xFF34C759); // Green/Blue
                }
            } else {
                binding.iconPriority.setVisibility(View.GONE);
            }

            // Repeat Icon
            boolean isRecurring = task.repeat != null && !task.repeat.isEmpty()
                    && !task.repeat.equalsIgnoreCase("NEVER");
            binding.iconRepeat.setVisibility(isRecurring ? View.VISIBLE : View.GONE);

            // Time / Reminder
            if (task.reminderTime > 0) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
                    binding.textTime.setText(sdf.format(task.reminderTime));
                    binding.textTime.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    binding.textTime.setVisibility(View.GONE);
                }
            } else {
                binding.textTime.setVisibility(View.GONE);
            }

            // Category (placeholder for now, or derive from logic if available)
            // For now, we'll hide it or show a default if we had one
            binding.textCategory.setVisibility(View.GONE);

            // Details Layout Visibility
            boolean hasDetails = hasPriority || isRecurring || (task.reminderTime > 0);
            binding.detailsLayout.setVisibility(hasDetails ? View.VISIBLE : View.GONE);

            // Checkbox
            // Apply accent color to checkbox
            android.content.SharedPreferences prefs = androidx.preference.PreferenceManager
                    .getDefaultSharedPreferences(binding.getRoot().getContext());
            int accentColor = prefs.getInt("accent_color", 0xFF34C759);
            binding.checkboxCompleted.setButtonTintList(android.content.res.ColorStateList.valueOf(accentColor));

            binding.checkboxCompleted.setOnCheckedChangeListener(null);
            binding.checkboxCompleted.setChecked(task.isCompleted);
            binding.checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    if (isChecked) {
                        task.completedTimestamp = System.currentTimeMillis();
                    } else {
                        task.completedTimestamp = 0;
                    }
                    listener.onTaskCompleted(task, isChecked);
                }
            });

            // Click Listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClicked(task);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onTaskLongClicked(task);
                }
                return true;
            });
        }
    }
}
