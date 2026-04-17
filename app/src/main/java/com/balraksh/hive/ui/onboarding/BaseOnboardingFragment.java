package com.balraksh.hive.ui.onboarding;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

abstract class BaseOnboardingFragment extends Fragment {

    private final List<Animator> runningAnimators = new ArrayList<>();

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) {
            view.post(() -> {
                if (isAdded() && getView() == view) {
                    startAnimations(view);
                }
            });
        }
    }

    @Override
    public void onPause() {
        stopAnimations();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        stopAnimations();
        super.onDestroyView();
    }

    protected abstract void startLoopAnimations(@NonNull View root);

    private void startAnimations(@NonNull View root) {
        stopAnimations();
        root.setAlpha(0f);
        root.setTranslationY(dp(18));

        AnimatorSet introAnimator = new AnimatorSet();
        introAnimator.playTogether(
                ObjectAnimator.ofFloat(root, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(root, View.TRANSLATION_Y, dp(18), 0f)
        );
        introAnimator.setDuration(420L);
        introAnimator.setInterpolator(new DecelerateInterpolator());
        introAnimator.start();
        trackAnimator(introAnimator);

        startLoopAnimations(root);
    }

    protected final void trackAnimator(Animator animator) {
        runningAnimators.add(animator);
    }

    protected final ObjectAnimator createPulseAnimator(View target, float minScale, float maxScale, long duration) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                target,
                PropertyValuesHolder.ofFloat(View.SCALE_X, minScale, maxScale),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, minScale, maxScale)
        );
        animator.setDuration(duration);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
        trackAnimator(animator);
        return animator;
    }

    protected final ObjectAnimator createAlphaPulseAnimator(View target, float minAlpha, float maxAlpha, long duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, View.ALPHA, minAlpha, maxAlpha);
        animator.setDuration(duration);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
        trackAnimator(animator);
        return animator;
    }

    protected final ObjectAnimator createFloatAnimator(View target, float fromY, float toY, long duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, fromY, toY);
        animator.setDuration(duration);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
        trackAnimator(animator);
        return animator;
    }

    protected final ObjectAnimator createRotateAnimator(View target, float fromDegrees, float toDegrees, long duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, View.ROTATION, fromDegrees, toDegrees);
        animator.setDuration(duration);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.RESTART);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
        trackAnimator(animator);
        return animator;
    }

    protected final ObjectAnimator createScaleXAnimator(View target, float fromScale, float toScale, long duration) {
        target.setPivotX(0f);
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, View.SCALE_X, fromScale, toScale);
        animator.setDuration(duration);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.RESTART);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
        trackAnimator(animator);
        return animator;
    }

    protected final void stopAnimations() {
        for (Animator animator : runningAnimators) {
            animator.cancel();
        }
        runningAnimators.clear();
    }

    protected final float dp(int value) {
        return value * Resources.getSystem().getDisplayMetrics().density;
    }
}

