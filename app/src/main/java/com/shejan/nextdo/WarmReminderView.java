package com.shejan.nextdo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class WarmReminderView extends LinearLayout {

    private Paint borderPaint;
    private Paint backgroundPaint;
    private RectF rectF;
    private float cornerRadius;

    private TextView timeTextView;
    private TextView amPmTextView;

    // Color Palette
    // Color Palette - Light (Warm Pastel) - UPDATED to Black per user request
    private static final int COLOR_ACCENT_LIGHT = 0xFF000000; // Black
    private static final int COLOR_BACKGROUND_LIGHT = 0xFFffe5c8; // Peach/Orange Surface (Keeping background)
    private static final int COLOR_ICON_BG_LIGHT = 0xFFf4e9dc; // Muted Clay
    private static final int COLOR_TEXT_LIGHT = 0xFF000000; // Black for Title
    private static final int COLOR_SUBTITLE_LIGHT = 0xFF666666; // Darker Gray for Subtitle
    private static final int COLOR_ICON_GRAY_LIGHT = 0xFF808080; // Gray for right icons

    // Color Palette - Dark (High Contrast)
    private static final int COLOR_ACCENT_DARK = 0xFFFFFFFF; // White
    private static final int COLOR_BACKGROUND_DARK = 0xFF000000; // Black
    private static final int COLOR_ICON_BG_DARK = 0xFF333333; // Dark Gray
    private static final int COLOR_TEXT_DARK = 0xFFFFFFFF; // White
    private static final int COLOR_SUBTITLE_DARK = 0xFFB0B0B0; // Light Gray
    private static final int COLOR_ICON_GRAY_DARK = 0xFFFFFFFF; // White

    public WarmReminderView(Context context) {
        super(context);
        init(context);
    }

    public WarmReminderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WarmReminderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int padding = dpToPx(16);
        setPadding(padding, padding, padding, padding);
        setWillNotDraw(false); // Enable onDraw for custom border

        // Detect Night Mode
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        // Select Colors
        int colorAccent = isNightMode ? COLOR_ACCENT_DARK : COLOR_ACCENT_LIGHT;
        int colorBackground = isNightMode ? COLOR_BACKGROUND_DARK : COLOR_BACKGROUND_LIGHT;
        int colorIconBg = isNightMode ? COLOR_ICON_BG_DARK : COLOR_ICON_BG_LIGHT;
        int colorText = isNightMode ? COLOR_TEXT_DARK : COLOR_TEXT_LIGHT;
        int colorSubtitle = isNightMode ? COLOR_SUBTITLE_DARK : COLOR_SUBTITLE_LIGHT;
        int colorIconGray = isNightMode ? COLOR_ICON_GRAY_DARK : COLOR_ICON_GRAY_LIGHT;

        cornerRadius = dpToPx(24);
        rectF = new RectF();

        // Setup Paints
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(colorAccent);
        borderPaint.setStrokeWidth(4f);
        borderPaint.setPathEffect(new DashPathEffect(new float[] { 20f, 20f }, 0f));

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(colorBackground);

        // 1. Icon Container (Circular)
        FrameLayout iconContainer = new FrameLayout(context);
        int iconSize = dpToPx(56);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.rightMargin = dpToPx(16);
        iconContainer.setLayoutParams(iconParams);

        // Circular Background for Icon
        android.graphics.drawable.GradientDrawable iconBg = new android.graphics.drawable.GradientDrawable();
        iconBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        iconBg.setColor(colorIconBg);
        iconContainer.setBackground(iconBg);

        // Icon
        ImageView iconView = new ImageView(context);
        int iconInnerSize = dpToPx(28);
        FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(iconInnerSize, iconInnerSize);
        imgParams.gravity = Gravity.CENTER;
        iconView.setLayoutParams(imgParams);
        iconView.setImageResource(R.drawable.ic_calendar); // Changed to Calendar icon
        iconView.setColorFilter(colorAccent);
        iconContainer.addView(iconView);

        addView(iconContainer);

        // 2. Text Container
        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, // Width 0 for weight
                ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.weight = 1; // Take available space
        textContainer.setLayoutParams(textParams);

        // Time Text (Title)
        timeTextView = new TextView(context);
        timeTextView.setText("Pick Date & Time");
        timeTextView.setTextColor(colorText);
        timeTextView.setTextSize(18); // Larger title
        timeTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        textContainer.addView(timeTextView);

        // AM/PM Text (Subtitle)
        amPmTextView = new TextView(context);
        amPmTextView.setText("Customize your\nschedule"); // Default subtitle with line break
        amPmTextView.setTextColor(colorSubtitle);
        amPmTextView.setTextSize(14);
        textContainer.addView(amPmTextView);

        addView(textContainer);

        // 3. Right Side Icons (Calendar & Bell)
        LinearLayout rightIcons = new LinearLayout(context);
        rightIcons.setOrientation(HORIZONTAL);
        rightIcons.setGravity(Gravity.CENTER_VERTICAL);

        ImageView smallCalendar = new ImageView(context);
        smallCalendar.setImageResource(R.drawable.ic_calendar);
        smallCalendar.setColorFilter(colorIconGray);
        int smallIconSize = dpToPx(20);
        LinearLayout.LayoutParams smallParams = new LinearLayout.LayoutParams(smallIconSize, smallIconSize);
        smallParams.rightMargin = dpToPx(12);
        smallCalendar.setLayoutParams(smallParams);
        rightIcons.addView(smallCalendar);

        ImageView smallBell = new ImageView(context);
        smallBell.setImageResource(R.drawable.ic_alarm); // Applying generic alarm icon as bell
        smallBell.setColorFilter(colorIconGray);
        smallBell.setLayoutParams(new LinearLayout.LayoutParams(smallIconSize, smallIconSize));
        rightIcons.addView(smallBell);

        addView(rightIcons);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Define bounds for background and border (inset by half stroke width for
        // border alignment)
        float halfStroke = 2f;
        rectF.set(halfStroke, halfStroke, getWidth() - halfStroke, getHeight() - halfStroke);

        // Draw Background
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint);

        // Draw Dashed Border
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint);

        super.onDraw(canvas);
    }

    public void setTime(String time, String amPm) {
        timeTextView.setText(time);
        amPmTextView.setText(amPm);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
