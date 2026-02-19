package com.shejan.nextdo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

/**
 * Custom CoordinatorLayout behavior that ensures the AppBar collapses fully
 * before the RecyclerView starts scrolling its content.
 *
 * Attach to the FrameLayout/RecyclerView container via:
 * app:layout_behavior="com.shejan.nextdo.AppBarFirstScrollBehavior"
 */
public class AppBarFirstScrollBehavior extends AppBarLayout.ScrollingViewBehavior {

    public AppBarFirstScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
            @NonNull View child,
            @NonNull View directTargetChild,
            @NonNull View target,
            int axes,
            int type) {
        // Always accept nested scroll so we can intercept it
        return super.onStartNestedScroll(coordinatorLayout, child, directTargetChild,
                target, axes, type);
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout,
            @NonNull View child,
            @NonNull View target,
            int dx, int dy,
            @NonNull int[] consumed,
            int type) {
        // Find the AppBarLayout in the CoordinatorLayout
        AppBarLayout appBarLayout = findAppBarLayout(coordinatorLayout);

        if (appBarLayout != null && dy > 0) {
            int totalScrollRange = appBarLayout.getTotalScrollRange();
            int currentOffset = Math.abs(appBarLayout.getTop());
            boolean isFullyCollapsed = currentOffset >= totalScrollRange;

            if (!isFullyCollapsed) {
                // AppBar not yet fully collapsed — consume ALL scroll for the AppBar
                // by passing it up to the super (which handles AppBar collapsing)
                // and marking the full dy as consumed so RecyclerView gets nothing
                super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
                // Force consume remaining scroll so RecyclerView doesn't scroll
                consumed[1] = dy;
                return;
            }
        }

        // AppBar is fully collapsed (or scrolling up) — let RecyclerView scroll
        // normally
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
    }

    @Override
    public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout,
            @NonNull View child,
            @NonNull View target,
            float velocityX, float velocityY) {
        // Find the AppBarLayout
        AppBarLayout appBarLayout = findAppBarLayout(coordinatorLayout);

        if (appBarLayout != null && velocityY > 0) {
            int totalScrollRange = appBarLayout.getTotalScrollRange();
            int currentOffset = Math.abs(appBarLayout.getTop());
            boolean isFullyCollapsed = currentOffset >= totalScrollRange;

            if (!isFullyCollapsed) {
                // Consume the fling — let AppBar handle collapsing, block RecyclerView fling
                appBarLayout.setExpanded(false, true);
                return true; // consumed
            }
        }

        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }

    private AppBarLayout findAppBarLayout(CoordinatorLayout coordinatorLayout) {
        for (int i = 0; i < coordinatorLayout.getChildCount(); i++) {
            View child = coordinatorLayout.getChildAt(i);
            if (child instanceof AppBarLayout) {
                return (AppBarLayout) child;
            }
        }
        return null;
    }
}
