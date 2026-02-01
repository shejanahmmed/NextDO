package com.shejan.nextdo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;

public class TaskSwipeCallback extends ItemTouchHelper.SimpleCallback {

    public interface SwipeActionReceiver {
        void onSwipedLeft(Task task, RecyclerView.ViewHolder viewHolder);

        void onSwipedRight(Task task, int position);
    }

    private final TaskListAdapter adapter;
    private final SwipeActionReceiver actionReceiver;
    private final WeakReference<Context> contextRef;

    // Drawing resources
    private final Paint textPaint = new Paint();
    private final Paint circlePaint = new Paint();
    private final Paint backgroundPaint = new Paint();
    private Drawable deleteIcon;
    private Drawable editIcon;

    // Constants
    private static final int DELETE_COLOR_START = Color.parseColor("#99E57373");
    private static final int DELETE_COLOR_END = Color.parseColor("#99EF5350");
    private static final int EDIT_COLOR_START = Color.parseColor("#9981C784");
    private static final int EDIT_COLOR_END = Color.parseColor("#9966BB6A");

    public TaskSwipeCallback(Context context, TaskListAdapter adapter, SwipeActionReceiver actionReceiver) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.contextRef = new WeakReference<>(context);
        this.adapter = adapter;
        this.actionReceiver = actionReceiver;

        initPaints();
    }

    private void initPaints() {
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.WHITE);

        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.WHITE);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.3f;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 0.5f;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            Task task = adapter.getTaskAt(position);
            Context context = contextRef.get();

            // Vibration feedback
            if (context != null) {
                try {
                    android.os.Vibrator vibrator = (android.os.Vibrator) context
                            .getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(50,
                                    android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(50);
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            if (direction == ItemTouchHelper.LEFT) {
                actionReceiver.onSwipedLeft(task, viewHolder);
            } else if (direction == ItemTouchHelper.RIGHT) {
                // Reset the item immediately to prevent removal for Edit action
                adapter.notifyItemChanged(position);
                actionReceiver.onSwipedRight(task, position);
            }
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
            boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        Context context = contextRef.get();

        // Initialize icons if needed
        if (deleteIcon == null && context != null) {
            deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_swipe_delete_custom);
        }
        if (editIcon == null && context != null) {
            editIcon = ContextCompat.getDrawable(context, R.drawable.ic_swipe_edit);
        }

        float swipeThreshold = itemView.getWidth() * 0.15f;
        float swipeProgress = Math.min(Math.abs(dX) / swipeThreshold, 1.0f);

        if (dX < 0) { // Swiping LEFT (Delete)
            drawSwipeFeedback(c, itemView, dX, swipeProgress, true);
        } else if (dX > 0) { // Swiping RIGHT (Edit)
            drawSwipeFeedback(c, itemView, dX, swipeProgress, false);
        } else {
            resetTransformations(itemView);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawSwipeFeedback(Canvas c, View itemView, float dX, float swipeProgress, boolean isDelete) {
        // Background
        int startColor = isDelete ? DELETE_COLOR_START : EDIT_COLOR_START;
        int endColor = isDelete ? DELETE_COLOR_END : EDIT_COLOR_END;
        backgroundPaint.setColor(interpolateColor(startColor, endColor, swipeProgress));

        float left = isDelete ? itemView.getRight() + dX : itemView.getLeft();
        float right = isDelete ? itemView.getRight() : itemView.getLeft() + dX;

        android.graphics.RectF backgroundRect = new android.graphics.RectF(left, itemView.getTop(), right,
                itemView.getBottom());
        c.drawRoundRect(backgroundRect, 30f, 30f, backgroundPaint);

        // Circle
        circlePaint.setAlpha((int) (100 * swipeProgress));
        float pulseRadius = 70f * swipeProgress * (1 + 0.3f * (float) Math.sin(System.currentTimeMillis() / 100.0));
        float centerX = isDelete ? itemView.getRight() - 120 : itemView.getLeft() + 120;
        float centerY = itemView.getTop() + (itemView.getHeight() / 2f);
        c.drawCircle(centerX, centerY, pulseRadius, circlePaint);

        // Icon
        Drawable icon = isDelete ? deleteIcon : editIcon;
        if (icon != null) {
            float iconSize = 70f * swipeProgress;
            int halfSize = (int) (iconSize / 2);
            icon.setBounds((int) (centerX - halfSize), (int) (centerY - halfSize),
                    (int) (centerX + halfSize), (int) (centerY + halfSize));
            icon.setAlpha((int) (255 * swipeProgress));
            icon.draw(c);
        }

        // Text
        textPaint.setTextSize(42f * swipeProgress);
        textPaint.setAlpha((int) (255 * swipeProgress));
        String text = isDelete ? "DELETE" : "EDIT";
        // Adjust text position relative to icon
        float textY = centerY + (70f * swipeProgress / 2) + 40;
        c.drawText(text, centerX, textY, textPaint);

        // Item Transformations
        float scale = 1.0f - (swipeProgress * 0.15f);
        float rotation = isDelete ? (swipeProgress * 8f) : (-swipeProgress * 8f);
        itemView.setScaleX(scale);
        itemView.setScaleY(scale);
        itemView.setRotation(rotation);
        itemView.setElevation(20f * swipeProgress);
    }

    private void resetTransformations(View itemView) {
        itemView.setScaleX(1.0f);
        itemView.setScaleY(1.0f);
        itemView.setRotation(0f);
        itemView.setElevation(0f);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        resetTransformations(viewHolder.itemView);
    }

    private int interpolateColor(int startColor, int endColor, float fraction) {
        int startA = Color.alpha(startColor);
        int startR = Color.red(startColor);
        int startG = Color.green(startColor);
        int startB = Color.blue(startColor);

        int endA = Color.alpha(endColor);
        int endR = Color.red(endColor);
        int endG = Color.green(endColor);
        int endB = Color.blue(endColor);

        return Color.argb(
                (int) (startA + fraction * (endA - startA)),
                (int) (startR + fraction * (endR - startR)),
                (int) (startG + fraction * (endG - startG)),
                (int) (startB + fraction * (endB - startB)));
    }
}
