package com.shejan.nextdo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;

public class PremiumSwipeCallback extends ItemTouchHelper.SimpleCallback {

    public interface SwipeActionListener {
        void onSwipeLeft(Task task, int position); // Delete

        void onSwipeRight(Task task, int position); // Edit
    }

    private final SwipeActionListener actionListener;
    private final WeakReference<Context> contextRef;
    private final TaskListAdapter adapter; // Can be DashboardTaskAdapter or TaskListAdapter

    // Drawing resources
    private final Paint backgroundPaint = new Paint();
    private Drawable deleteIcon;
    private Drawable editIcon;

    // Constants
    private static final int DELETE_COLOR = Color.parseColor("#EF5350"); // Red
    private static final int EDIT_COLOR = Color.parseColor("#66BB6A"); // Green

    // State
    private int currentlySwipedPosition = -1;
    private static final float SWIPE_THRESHOLD = 0.3f; // Release past 30% to trigger

    public PremiumSwipeCallback(Context context, TaskListAdapter adapter, SwipeActionListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.contextRef = new WeakReference<>(context);
        this.adapter = adapter;
        this.actionListener = listener;
        this.taskAccessor = new TaskAccessor() {
            @Override
            public Task getTaskAt(int position) {
                return adapter.getTaskAt(position);
            }

            @Override
            public void notifyChanged(int position) {
                adapter.notifyItemChanged(position);
            }
        };
        initPaints();
    }

    // Overloaded constructor for DashboardTaskAdapter (Generic solution would be
    // better but keeping it simple for now)
    // Actually, let's use a common interface or just pass the adapter as Object and
    // cast if needed,
    // but better yet, let's assume the caller handles the data lookup if we just
    // pass position.
    // However, onSwiped gives us ViewHolder, so we can access position.
    // We need 'adapter' mainly to get the Task object.
    // Let's make a helper interface for the Adapter if strictly needed, but for now
    // let's pass a generic interface for getting the task.

    public interface TaskAccessor {
        Task getTaskAt(int position);

        void notifyChanged(int position); // To reset swipe
    }

    private final TaskAccessor taskAccessor;

    public PremiumSwipeCallback(Context context, TaskAccessor taskAccessor, SwipeActionListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.contextRef = new WeakReference<>(context);
        this.taskAccessor = taskAccessor;
        this.actionListener = listener;
        this.adapter = null; // Unused in this mode
        initPaints();
    }

    private void initPaints() {
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            Task task = taskAccessor.getTaskAt(position);

            // Reset the item view immediately so it doesn't stay swiped out
            taskAccessor.notifyChanged(position);

            if (direction == ItemTouchHelper.LEFT) {
                actionListener.onSwipeLeft(task, position);
            } else if (direction == ItemTouchHelper.RIGHT) {
                actionListener.onSwipeRight(task, position);
            }
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
            boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        Context context = contextRef.get();

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Initialize icons if needed
            if (deleteIcon == null && context != null) {
                deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_swipe_delete_custom);
            }
            if (editIcon == null && context != null) {
                editIcon = ContextCompat.getDrawable(context, R.drawable.ic_swipe_edit);
            }

            float width = itemView.getWidth();
            float progress = Math.abs(dX) / width;

            // Background Logic
            if (dX != 0) {
                int color = dX > 0 ? EDIT_COLOR : DELETE_COLOR;
                backgroundPaint.setColor(color);

                // Draw rounded background
                float left = dX > 0 ? itemView.getLeft() : itemView.getRight() + dX;
                float right = dX > 0 ? itemView.getLeft() + dX : itemView.getRight();

                // Draw Rect
                // c.drawRect(left, itemView.getTop(), right, itemView.getBottom(),
                // backgroundPaint);

                // Better visualization: Draw background behind the item (simulated by drawing
                // before super,
                // but ItemTouchHelper draws on top. So we draw on the canvas directly).
                // Actually, standard swipe draws background underneath.

                android.graphics.RectF backgroundRect = new android.graphics.RectF(
                        dX > 0 ? itemView.getLeft() : itemView.getRight() + dX,
                        itemView.getTop(),
                        dX > 0 ? itemView.getLeft() + dX : itemView.getRight(),
                        itemView.getBottom());

                // Corner radius to match cards (approx 16dp)
                float radius = 40f;
                c.drawRoundRect(backgroundRect, radius, radius, backgroundPaint);

                // Icon Logic
                Drawable icon = dX > 0 ? editIcon : deleteIcon;
                if (icon != null) {
                    int iconMargin = (int) ((itemView.getHeight() - icon.getIntrinsicHeight()) / 2);
                    int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int iconBottom = iconTop + icon.getIntrinsicHeight();
                    int iconLeft, iconRight;

                    if (dX > 0) { // Swiping Right
                        iconLeft = itemView.getLeft() + iconMargin;
                        iconRight = iconLeft + icon.getIntrinsicWidth();
                    } else { // Swiping Left
                        iconRight = itemView.getRight() - iconMargin;
                        iconLeft = iconRight - icon.getIntrinsicWidth();
                    }

                    // Only draw icon if there is space
                    if (Math.abs(dX) > iconMargin + icon.getIntrinsicWidth()) {
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        icon.draw(c);
                    }
                }
            }

            // Apply resistance (Spring effect)
            // We want dX to move slower as it gets further
            // But ItemTouchHelper controls dX directly based on touch.
            // To effectively "resist", we would need to override dX which we can't easily
            // do here for the view translation
            // without fighting the framework, OR we rely on standard translation but
            // limited.
            // "Spring loaded" usually means it snaps back. ItemTouchHelper SimpleCallback
            // snaps back if not "swiped".
            // Implementation of standard "Spring" feel in ItemTouchHelper:
            // The default ItemTouchHelper behavior IS springy if you don't call dismiss.
            // Since we reset the view in onSwiped, it will bounce back.

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return SWIPE_THRESHOLD;
    }
}
