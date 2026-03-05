package com.shejan.nextdo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.shejan.nextdo.databinding.ItemTaskMinimalBinding;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class TaskListAdapter extends ListAdapter<Task, TaskListAdapter.TaskViewHolder> {

    private static final String REPEAT_NEVER = "NEVER";

    public int expandedPosition = RecyclerView.NO_POSITION;

    private final OnTaskInteractionListener listener;

    // Cache SimpleDateFormat
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public interface OnTaskInteractionListener {
        void onTaskCompleted(Task task, boolean isCompleted);

        void onTaskClicked(Task task);

        void onTaskLongClicked(Task task);

        void onTaskDelete(Task task);
    }

    public TaskListAdapter(@NonNull DiffUtil.ItemCallback<Task> diffCallback, OnTaskInteractionListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTaskMinimalBinding binding = ItemTaskMinimalBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false);
        return new TaskViewHolder(binding, dateFormat);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task current = getItem(position);
        holder.bind(current, listener, this);
    }

    public Task getTaskAt(int position) {
        return getItem(position);
    }

    @Override
    public void submitList(java.util.List<Task> list) {
        expandedPosition = RecyclerView.NO_POSITION;
        super.submitList(list);
    }

    public void expandView(View view) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).setListener(null);
    }

    public void collapseView(View view) {
        view.animate().alpha(0f).setDuration(200).setListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                view.setVisibility(View.GONE);
                view.setAlpha(1f);
            }
        });
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

                    oldItem.reminderTime == newItem.reminderTime &&
                    Objects.equals(oldItem.repeat, newItem.repeat) &&
                    oldItem.isCompleted == newItem.isCompleted;
        }
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemTaskMinimalBinding binding;
        private final SimpleDateFormat dateFormat;

        private TaskViewHolder(ItemTaskMinimalBinding binding, SimpleDateFormat dateFormat) {
            super(binding.getRoot());
            this.binding = binding;
            this.dateFormat = dateFormat;

        }

        public void bind(final Task task, final OnTaskInteractionListener listener, TaskListAdapter adapter) {
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

            // Repeat Icon
            boolean isRecurring = task.repeat != null && !task.repeat.isEmpty()
                    && !task.repeat.equalsIgnoreCase(REPEAT_NEVER);
            binding.iconRepeat.setVisibility(isRecurring ? View.VISIBLE : View.GONE);

            // Time / Reminder
            if (task.reminderTime > 0) {
                try {
                    // Use passed date format
                    binding.textTime.setText(dateFormat.format(task.reminderTime));
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
            boolean hasDetails = isRecurring || (task.reminderTime > 0);
            binding.detailsLayout.setVisibility(hasDetails ? View.VISIBLE : View.GONE);

            // Expandable Layout Logic
            int adapterPosition = getBindingAdapterPosition();
            boolean isExpanded = adapterPosition == adapter.expandedPosition;

            binding.layoutExpandable.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            binding.layoutExpandable.setAlpha(isExpanded ? 1f : 0f);

            if (task.description != null && !task.description.isEmpty()) {
                binding.textTaskDescription.setText(task.description);
                binding.textTaskDescription.setVisibility(View.VISIBLE);
            } else {
                binding.textTaskDescription.setVisibility(View.GONE);
            }

            // Alternating Background Colors
            int colorResId;
            if (adapterPosition == RecyclerView.NO_POSITION) {
                adapterPosition = 0;
            }
            // Use modulo arithmetic to cycle through 3 colors
            switch (adapterPosition % 3) {
                case 0:
                    colorResId = R.color.task_card_bg_1;
                    break;
                case 1:
                    colorResId = R.color.task_card_bg_2;
                    break;
                case 2:
                    colorResId = R.color.task_card_bg_3;
                    break;
                default:
                    colorResId = R.color.task_card_bg_1;
                    break;
            }

            // Apply color to the task card container's background
            // We must invalidate the view to ensure the redraw happens
            int color = androidx.core.content.ContextCompat.getColor(itemView.getContext(), colorResId);

            // Fix: Target the taskCardContainer which actually has the background
            if (binding.taskCardContainer.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
                android.graphics.drawable.GradientDrawable bg = (android.graphics.drawable.GradientDrawable) binding.taskCardContainer
                        .getBackground().mutate();
                bg.setColor(color);
                binding.taskCardContainer.setBackground(bg);
            } else {
                // Fallback for simple shapes or ColorDrawables if GradientDrawable cast fails
                // Using tint list is safer for generic drawables, but setColor on
                // GradientDrawable is most robust for Shapes
                binding.taskCardContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            }

            // Click Listeners
            binding.taskCardContainer.setOnClickListener(v -> {
                int currentPosition = getBindingAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION)
                    return;

                int previousExpandedPosition = adapter.expandedPosition;
                if (adapter.expandedPosition == currentPosition) {
                    adapter.expandedPosition = RecyclerView.NO_POSITION;
                    adapter.collapseView(binding.layoutExpandable);
                } else {
                    adapter.expandedPosition = currentPosition;
                    adapter.expandView(binding.layoutExpandable);
                    if (previousExpandedPosition != RecyclerView.NO_POSITION) {
                        adapter.notifyItemChanged(previousExpandedPosition);
                    }
                }
            });

            binding.btnEdit.setOnClickListener(v -> {
                if (listener != null)
                    listener.onTaskClicked(task);
            });

            // Set button text based on completion status (Styling remains the same for
            // both)
            if (task.isCompleted) {
                binding.btnDone.setText("Undone");
            } else {
                binding.btnDone.setText("Done");
            }

            // Set uniform style for both Done and Undone states
            binding.btnDone.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    MaterialColors.getColor(binding.btnDone, com.google.android.material.R.attr.colorOnSurface)));
            binding.btnDone.setTextColor(
                    MaterialColors.getColor(binding.btnDone, com.google.android.material.R.attr.colorSurface));

            binding.btnDone.setOnClickListener(v -> {
                if (listener != null) {
                    adapter.expandedPosition = RecyclerView.NO_POSITION;

                    if (task.isCompleted) {
                        task.completedTimestamp = 0;
                        listener.onTaskCompleted(task, false);
                    } else {
                        task.completedTimestamp = System.currentTimeMillis();
                        listener.onTaskCompleted(task, true);
                    }
                }
            });

            binding.btnDeleteTask.setOnClickListener(v -> {
                if (listener != null) {
                    adapter.expandedPosition = RecyclerView.NO_POSITION;
                    listener.onTaskDelete(task);
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
