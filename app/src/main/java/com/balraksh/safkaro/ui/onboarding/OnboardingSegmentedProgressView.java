package com.balraksh.safkaro.ui.onboarding;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.balraksh.safkaro.R;

import java.util.ArrayList;
import java.util.List;

public class OnboardingSegmentedProgressView extends LinearLayout {

    private final List<View> fillViews = new ArrayList<>();
    private ValueAnimator progressAnimator;
    private int segmentCount;

    public OnboardingSegmentedProgressView(Context context) {
        this(context, null);
    }

    public OnboardingSegmentedProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
    }

    public void setSegmentCount(int count) {
        if (segmentCount == count) {
            return;
        }
        segmentCount = count;
        removeAllViews();
        fillViews.clear();

        int spacing = getResources().getDimensionPixelSize(R.dimen.space_6);
        int height = getResources().getDimensionPixelSize(R.dimen.onboarding_progress_height);
        for (int i = 0; i < count; i++) {
            FrameLayout track = new FrameLayout(getContext());
            LayoutParams params = new LayoutParams(0, height, 1f);
            if (i < count - 1) {
                params.setMarginEnd(spacing);
            }
            track.setLayoutParams(params);
            track.setBackgroundResource(R.drawable.bg_onboarding_progress_track);

            View fill = new View(getContext());
            FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
            );
            fill.setLayoutParams(fillParams);
            fill.setBackgroundResource(R.drawable.bg_onboarding_progress_fill);
            fill.setPivotX(0f);
            fill.setScaleX(0f);
            track.addView(fill);

            fillViews.add(fill);
            addView(track);
        }
    }

    public void bindStaticState(int currentIndex) {
        stopAnimation();
        for (int i = 0; i < fillViews.size(); i++) {
            View fill = fillViews.get(i);
            fill.setScaleX(i <= currentIndex ? 1f : 0f);
        }
    }

    public void animateSegment(int currentIndex, long durationMillis) {
        bindStaticState(currentIndex);
        if (currentIndex < 0 || currentIndex >= fillViews.size()) {
            return;
        }
        View currentFill = fillViews.get(currentIndex);
        progressAnimator = ValueAnimator.ofFloat(0f, 1f);
        progressAnimator.setDuration(durationMillis);
        progressAnimator.addUpdateListener(animation ->
                currentFill.setScaleX((Float) animation.getAnimatedValue())
        );
        progressAnimator.start();
    }

    public void stopAnimation() {
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }
    }
}
