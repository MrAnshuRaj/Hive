package com.balraksh.hive.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.balraksh.hive.R;
import com.balraksh.hive.utils.AppLaunchRouter;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private static final long PROGRESS_DURATION_MS = 3200L;

    private final List<Animator> runningAnimators = new ArrayList<>();
    private boolean hasNavigated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View pattern = findViewById(R.id.imageSplashPattern);
        View sideGlowStart = findViewById(R.id.viewSplashEdgeGlowStart);
        View sideGlowEnd = findViewById(R.id.viewSplashEdgeGlowEnd);
        View particleOne = findViewById(R.id.viewSplashParticleOne);
        View particleTwo = findViewById(R.id.viewSplashParticleTwo);
        MaterialCardView iconCard = findViewById(R.id.cardSplashIcon);
        View iconGlow = findViewById(R.id.viewSplashIconGlow);
        View iconMark = findViewById(R.id.imageSplashHex);
        TextView title = findViewById(R.id.textSplashTitle);
        View tagline = findViewById(R.id.cardSplashTagline);
        ProgressBar progressBar = findViewById(R.id.progressSplash);
        TextView status = findViewById(R.id.textSplashStatus);

        title.setAlpha(0f);
        title.setTranslationY(dp(18));
        tagline.setAlpha(0f);
        tagline.setTranslationY(dp(16));
        status.setAlpha(0f);
        status.setTranslationY(dp(12));
        iconCard.setAlpha(0f);
        iconCard.setScaleX(0.86f);
        iconCard.setScaleY(0.86f);

        startLoopAnimator(createPulseAnimator(iconGlow, 0.92f, 1.08f, 1700L));
        startLoopAnimator(createAlphaAnimator(iconGlow, 0.36f, 0.74f, 1700L));
        startLoopAnimator(createPulseAnimator(iconMark, 0.97f, 1.05f, 1300L));
        startLoopAnimator(createAlphaAnimator(pattern, 0.13f, 0.24f, 2600L));
        startLoopAnimator(createAlphaAnimator(sideGlowStart, 0.18f, 0.34f, 2400L));
        startLoopAnimator(createAlphaAnimator(sideGlowEnd, 0.14f, 0.3f, 2400L));
        startLoopAnimator(createFloatAnimator(particleOne, -dp(10), dp(8), 2600L));
        startLoopAnimator(createAlphaAnimator(particleOne, 0.2f, 0.8f, 2600L));
        startLoopAnimator(createFloatAnimator(particleTwo, dp(10), -dp(6), 3000L));
        startLoopAnimator(createAlphaAnimator(particleTwo, 0.14f, 0.62f, 3000L));
        iconCard.post(() -> {
            if (!isFinishing() && !isDestroyed()) {
                startEntrance(iconCard, title, tagline, status);
                startProgress(progressBar);
            }
        });
    }

    private void startEntrance(@NonNull View iconCard, @NonNull View title, @NonNull View tagline,
                               @NonNull View status) {
        AnimatorSet iconSet = new AnimatorSet();
        iconSet.playTogether(
                ObjectAnimator.ofFloat(iconCard, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(iconCard, View.SCALE_X, 0.86f, 1f),
                ObjectAnimator.ofFloat(iconCard, View.SCALE_Y, 0.86f, 1f)
        );
        iconSet.setDuration(560L);
        iconSet.setInterpolator(new DecelerateInterpolator());
        iconSet.start();
        runningAnimators.add(iconSet);

        AnimatorSet titleSet = new AnimatorSet();
        titleSet.playTogether(
                ObjectAnimator.ofFloat(title, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(title, View.TRANSLATION_Y, dp(18), 0f)
        );
        titleSet.setStartDelay(180L);
        titleSet.setDuration(500L);
        titleSet.setInterpolator(new DecelerateInterpolator());
        titleSet.start();
        runningAnimators.add(titleSet);

        AnimatorSet taglineSet = new AnimatorSet();
        taglineSet.playTogether(
                ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(tagline, View.TRANSLATION_Y, dp(16), 0f)
        );
        taglineSet.setStartDelay(320L);
        taglineSet.setDuration(480L);
        taglineSet.setInterpolator(new DecelerateInterpolator());
        taglineSet.start();
        runningAnimators.add(taglineSet);

        AnimatorSet statusSet = new AnimatorSet();
        statusSet.playTogether(
                ObjectAnimator.ofFloat(status, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(status, View.TRANSLATION_Y, dp(12), 0f)
        );
        statusSet.setStartDelay(420L);
        statusSet.setDuration(460L);
        statusSet.setInterpolator(new DecelerateInterpolator());
        statusSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                startLoopAnimator(createAlphaAnimator(status, 0.56f, 1f, 1200L));
            }
        });
        statusSet.start();
        runningAnimators.add(statusSet);
    }

    private void startProgress(@NonNull ProgressBar progressBar) {
        progressBar.setProgress(0);
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        progressAnimator.setDuration(PROGRESS_DURATION_MS);
        progressAnimator.setStartDelay(260L);
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                navigateForward();
            }
        });
        progressAnimator.start();
        runningAnimators.add(progressAnimator);
    }

    private void navigateForward() {
        if (hasNavigated || isFinishing()) {
            return;
        }
        hasNavigated = true;
        Intent intent = AppLaunchRouter.createLaunchIntent(this);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startLoopAnimator(@NonNull ObjectAnimator animator) {
        animator.start();
        runningAnimators.add(animator);
    }

    private ObjectAnimator createPulseAnimator(@NonNull View target, float minScale, float maxScale, long duration) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                target,
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, minScale, maxScale),
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, minScale, maxScale)
        );
        animator.setDuration(duration);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        return animator;
    }

    private ObjectAnimator createAlphaAnimator(@NonNull View target, float from, float to, long duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, View.ALPHA, from, to);
        animator.setDuration(duration);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        return animator;
    }

    private ObjectAnimator createFloatAnimator(@NonNull View target, float from, float to, long duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, from, to);
        animator.setDuration(duration);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        return animator;
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDestroy() {
        for (Animator animator : runningAnimators) {
            animator.cancel();
        }
        runningAnimators.clear();
        super.onDestroy();
    }
}

