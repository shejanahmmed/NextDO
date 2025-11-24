package com.shejan.nextdo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.shejan.nextdo.databinding.RecyclerviewItemBinding;

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
        RecyclerviewItemBinding binding = RecyclerviewItemBinding.inflate(LayoutInflater.from(parent.getContext()),
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

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final RecyclerviewItemBinding binding;

        private TaskViewHolder(RecyclerviewItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final Task task, final OnTaskInteractionListener listener) {
            if (task == null)
                return;

            if (binding.textTitle != null) {
                binding.textTitle.setText(task.title != null ? task.title : "");
            }

            if (binding.textDescription != null) {
                binding.textDescription.setText(task.description != null ? task.description : "");
                binding.textDescription.setVisibility(
                        task.description != null && !task.description.isEmpty() ? View.VISIBLE : View.GONE);
            }

            // DEFINITIVE FIX: Removing conditional styling for completed tasks.
            android.graphics.drawable.Drawable background = androidx.core.content.ContextCompat
                    .getDrawable(binding.getRoot().getContext(), R.drawable.nothing_card_bg);
            if (background != null) {
                background.mutate().setAlpha(180); // Make it more transparent
                binding.getRoot().setBackground(background);
            }

            if (binding.chipPriority != null) {
                if (task.priority != null && !task.priority.isEmpty() && !task.priority.equalsIgnoreCase("NONE")) {
                    binding.chipPriority.setText(task.priority);
                    binding.chipPriority.setVisibility(View.VISIBLE);
                } else {
                    binding.chipPriority.setVisibility(View.GONE);
                }
            }

            if (binding.textReminder != null) {
                if (task.reminderTime > 0) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
                        binding.textReminder.setText(sdf.format(task.reminderTime));
                        binding.textReminder.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        binding.textReminder.setVisibility(View.GONE);
                    }
                } else {
                    binding.textReminder.setVisibility(View.GONE);
                }
            }

            if (binding.detailsLayout != null) {
                boolean hasDetails = (task.priority != null && !task.priority.isEmpty()
                        && !task.priority.equalsIgnoreCase("NONE")) || task.reminderTime > 0;
                binding.detailsLayout.setVisibility(hasDetails ? View.VISIBLE : View.GONE);
            }

            binding.checkboxCompleted.setOnCheckedChangeListener(null);
            binding.checkboxCompleted.setChecked(task.isCompleted);
            binding.checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onTaskCompleted(task, isChecked);
                }
            });

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
