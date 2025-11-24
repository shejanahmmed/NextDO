package com.shejan.nextdo;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class TopFadeRecyclerView extends RecyclerView {

    public TopFadeRecyclerView(@NonNull Context context) {
        super(context);
        init();
    }

    public TopFadeRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TopFadeRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(200); // 80dp in pixels approximately
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        // Only show fade if we can actually scroll up (i.e., we've scrolled down)
        if (canScrollVertically(-1)) {
            return 1.0f; // Full fade at top when scrolled
        }
        return 0.0f; // No fade when at the very top
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        return 0.0f; // No fade at bottom
    }
}
