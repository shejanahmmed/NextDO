package com.shejan.nextdo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.shejan.nextdo.databinding.ItemTaskMinimalBinding;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class TaskListAdapter extends ListAdapter<Task, TaskListAdapter.TaskViewHolder> {

    private static final String REPEAT_NEVER = "NEVER";

    private final OnTaskInteractionListener listener;

    // Cache SimpleDateFormat
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

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
        ItemTaskMinimalBinding binding = ItemTaskMinimalBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false);
        return new TaskViewHolder(binding, dateFormat);
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

            // Set initial accent color
            int accentColor = androidx.core.content.ContextCompat.getColor(binding.getRoot().getContext(),
                    R.color.action_button_color);
            binding.checkboxCompleted.setButtonTintList(android.content.res.ColorStateList.valueOf(accentColor));
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

            // Checkbox - Use static accent color
            int accentColor = androidx.core.content.ContextCompat.getColor(binding.getRoot().getContext(),
                    R.color.action_button_color);
            binding.checkboxCompleted.setButtonTintList(android.content.res.ColorStateList.valueOf(accentColor));

            // Alternating Background Colors
            int colorResId;
            int adapterPosition = getBindingAdapterPosition();
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

            binding.checkboxCompleted.setOnCheckedChangeListener(null);
            binding.checkboxCompleted.setChecked(task.isCompleted);
            binding.checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    // Update timestamp on completion toggle
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
