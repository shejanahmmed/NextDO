package com.shejan.nextdo;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class RecycleBinAdapter extends ListAdapter<Task, RecycleBinAdapter.TaskViewHolder> {

    private final OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onRestore(Task task);

        void onDelete(Task task);
    }

    public RecycleBinAdapter(@NonNull DiffUtil.ItemCallback<Task> diffCallback,
            OnTaskActionListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recycle_bin, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task current = getItem(position);
        holder.bind(current, listener);
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView taskItemView;
        private final TextView taskDescriptionView;
        private final View btnRestore;
        private final View btnDelete;

        public TaskViewHolder(View itemView) {
            super(itemView);
            taskItemView = itemView.findViewById(R.id.text_title);
            taskDescriptionView = itemView.findViewById(R.id.text_description);
            btnRestore = itemView.findViewById(R.id.btn_restore);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        // Revised bind method to accept listener
        public void bind(Task task, OnTaskActionListener listener) {
            taskItemView.setText(task.title);

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy",
                    java.util.Locale.getDefault());
            String deletedDate = sdf.format(new java.util.Date(task.deletedTimestamp));

            long diff = System.currentTimeMillis() - task.deletedTimestamp;
            long daysLeft = 30 - java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);

            if (daysLeft < 0)
                daysLeft = 0;

            Context context = itemView.getContext();
            taskDescriptionView.setText(context.getString(R.string.deleted_task_description, deletedDate, daysLeft));

            // Strike through title
            taskItemView.setPaintFlags(taskItemView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            taskItemView.setAlpha(0.7f);

            btnRestore.setOnClickListener(v -> {
                if (listener != null)
                    listener.onRestore(task);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null)
                    listener.onDelete(task);
            });
        }
    }

    static class TaskDiff extends DiffUtil.ItemCallback<Task> {

        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.title.equals(newItem.title) &&
                    oldItem.description.equals(newItem.description) &&
                    oldItem.priority.equals(newItem.priority) &&
                    oldItem.reminderTime == newItem.reminderTime &&
                    oldItem.isCompleted == newItem.isCompleted &&
                    oldItem.isDeleted == newItem.isDeleted;
        }
    }
}
