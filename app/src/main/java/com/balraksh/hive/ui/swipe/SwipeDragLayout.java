package com.balraksh.hive.ui.swipe;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SwipeDragLayout extends FrameLayout {

    public interface Callback {
        boolean canStartSwipe();

        void onSwipeDrag(float translationX, float translationY);

        void onSwipeRelease(float translationX, float translationY, float velocityX);
    }

    private final int touchSlop;
    private Callback callback;

    private float downX;
    private float downY;
    private long downTime;
    private boolean dragging;

    public SwipeDragLayout(@NonNull Context context) {
        this(context, null);
    }

    public SwipeDragLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (callback == null || !callback.canStartSwipe()) {
            return super.onInterceptTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getRawX();
                downY = event.getRawY();
                downTime = event.getEventTime();
                dragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downX;
                float dy = event.getRawY() - downY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    dragging = true;
                    requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                dragging = false;
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (callback == null || !callback.canStartSwipe()) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downX;
                float dy = (event.getRawY() - downY) * 0.12f;
                callback.onSwipeDrag(dx, dy);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float finalDx = event.getRawX() - downX;
                float finalDy = (event.getRawY() - downY) * 0.12f;
                long elapsed = Math.max(1L, event.getEventTime() - downTime);
                float velocityX = (finalDx * 1000f) / elapsed;
                callback.onSwipeRelease(finalDx, finalDy, velocityX);
                dragging = false;
                return true;
            default:
                return true;
        }
    }
}
