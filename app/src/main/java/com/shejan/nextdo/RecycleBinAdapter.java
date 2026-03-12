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
        holder.bind(current, listener, position);
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView taskItemView;
        private final TextView taskDescriptionView;
        private final TextView textCountdownVal;
        private final View btnRestore;
        private final View btnDelete;
        private final androidx.cardview.widget.CardView cardRoot;
        private final View iconContainer;

        public TaskViewHolder(View itemView) {
            super(itemView);
            taskItemView = itemView.findViewById(R.id.text_title);
            taskDescriptionView = itemView.findViewById(R.id.text_description);
            textCountdownVal = itemView.findViewById(R.id.text_countdown_val);
            btnRestore = itemView.findViewById(R.id.btn_restore);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            cardRoot = itemView.findViewById(R.id.card_root);
            iconContainer = itemView.findViewById(R.id.icon_container);
        }

        public void bind(Task task, OnTaskActionListener listener, int position) {
            // Apply rotating background colors to the card for a professional yet fresh look
            int colorResId;
            switch (position % 3) {
                case 0:
                    colorResId = R.color.task_card_bg_1;
                    break;
                case 1:
                    colorResId = R.color.task_card_bg_2;
                    break;
                default:
                    colorResId = R.color.task_card_bg_3;
                    break;
            }
            int color = androidx.core.content.ContextCompat.getColor(itemView.getContext(), colorResId);
            if (cardRoot != null) {
                cardRoot.setCardBackgroundColor(color);
            }

            taskItemView.setText(task.title);

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy",
                    java.util.Locale.getDefault());
            String deletedDate = sdf.format(new java.util.Date(task.deletedTimestamp));

            long diff = System.currentTimeMillis() - task.deletedTimestamp;
            long daysLeft = 30 - java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);

            if (daysLeft < 0)
                daysLeft = 0;

            taskDescriptionView.setText("Deleted on " + deletedDate);
            if (textCountdownVal != null) {
                textCountdownVal.setText(daysLeft + " days left");
            }

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

                    oldItem.reminderTime == newItem.reminderTime &&
                    oldItem.isCompleted == newItem.isCompleted &&
                    oldItem.isDeleted == newItem.isDeleted;
        }
    }
}
