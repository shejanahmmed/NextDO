package com.shejan.nextdo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Random;

public class EqualizerView extends View {
    private static final int BAR_COUNT = 5;
    private static final int BAR_WIDTH_DP = 4;
    private static final int BAR_SPACING_DP = 4;
    private static final int MAX_BAR_HEIGHT_DP = 24;
    private static final int MIN_BAR_HEIGHT_DP = 4;

    private Paint paint;
    private float[] barHeights;
    private ValueAnimator animator;
    private Random random;
    private int barWidth;
    private int barSpacing;
    private int maxBarHeight;
    private int minBarHeight;

    public EqualizerView(Context context) {
        super(context);
        init();
    }

    public EqualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        // Use theme color or default
        paint.setColor(0xFF6200EE); // Purple color, you can change this

        barHeights = new float[BAR_COUNT];
        random = new Random();

        // Convert DP to pixels
        float density = getResources().getDisplayMetrics().density;
        barWidth = (int) (BAR_WIDTH_DP * density);
        barSpacing = (int) (BAR_SPACING_DP * density);
        maxBarHeight = (int) (MAX_BAR_HEIGHT_DP * density);
        minBarHeight = (int) (MIN_BAR_HEIGHT_DP * density);

        // Initialize with random heights
        for (int i = 0; i < BAR_COUNT; i++) {
            barHeights[i] = minBarHeight + random.nextFloat() * (maxBarHeight - minBarHeight);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = (barWidth + barSpacing) * BAR_COUNT;
        int height = maxBarHeight;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerY = getHeight() / 2;

        for (int i = 0; i < BAR_COUNT; i++) {
            float x = i * (barWidth + barSpacing);
            float barHeight = barHeights[i];

            // Draw bar centered vertically
            float top = centerY - barHeight / 2;
            float bottom = centerY + barHeight / 2;

            canvas.drawRoundRect(
                    x,
                    top,
                    x + barWidth,
                    bottom,
                    barWidth / 2f,
                    barWidth / 2f,
                    paint);
        }
    }

    public void startAnimation() {
        if (animator != null && animator.isRunning()) {
            return;
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(animation -> {
            // Randomly update bar heights for wave effect
            for (int i = 0; i < BAR_COUNT; i++) {
                // Smooth transition
                float target = minBarHeight + random.nextFloat() * (maxBarHeight - minBarHeight);
                barHeights[i] = barHeights[i] * 0.85f + target * 0.15f;
            }
            invalidate();
        });

        animator.start();
    }

    public void stopAnimation() {
        if (animator != null) {
            animator.cancel();
        }

        // Reset to minimum heights
        for (int i = 0; i < BAR_COUNT; i++) {
            barHeights[i] = minBarHeight;
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}
