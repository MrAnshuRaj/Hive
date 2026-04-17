package com.balraksh.hive.ui.onboarding;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

import com.balraksh.hive.R;
import com.balraksh.hive.repository.OnboardingPreferences;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.utils.AppLaunchRouter;

public class OnboardingActivity extends BaseEdgeToEdgeActivity {

    private static final int TOTAL_SLIDE_COUNT = 6;

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            currentPage = position;
            bindPageState(position);
        }
    };

    private ViewPager2 viewPager;
    private OnboardingSegmentedProgressView segmentedProgressView;
    private TextView skipTextView;
    private TextView legalTextView;
    private MaterialButton ctaButton;
    private View ctaShineView;
    private View patternView;
    private OnboardingPagerAdapter pagerAdapter;
    private OnboardingPreferences onboardingPreferences;
    private ObjectAnimator shineAnimator;
    private AnimatorSet patternAnimator;
    private int currentPage;
    private boolean completingOnboarding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_onboarding);

        onboardingPreferences = new OnboardingPreferences(this);
        segmentedProgressView = findViewById(R.id.viewOnboardingProgress);
        skipTextView = findViewById(R.id.textOnboardingSkip);
        legalTextView = findViewById(R.id.textOnboardingLegal);
        ctaButton = findViewById(R.id.buttonOnboardingCta);
        ctaShineView = findViewById(R.id.viewOnboardingCtaShine);
        patternView = findViewById(R.id.imageOnboardingPattern);
        viewPager = findViewById(R.id.viewPagerOnboarding);

        segmentedProgressView.setSegmentCount(TOTAL_SLIDE_COUNT);

        pagerAdapter = new OnboardingPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageTransformer(new OnboardingPageTransformer());
        viewPager.setOffscreenPageLimit(1);
        viewPager.registerOnPageChangeCallback(pageChangeCallback);

        skipTextView.setOnClickListener(v -> completeOnboarding());
        ctaButton.setOnClickListener(v -> {
            if (completingOnboarding) {
                return;
            }
            if (currentPage < TOTAL_SLIDE_COUNT - 1) {
                viewPager.setCurrentItem(currentPage + 1, true);
            } else {
                completeOnboarding();
            }
        });

        currentPage = 0;
        bindPageState(currentPage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeStartPatternBreathing();
    }

    @Override
    protected void onPause() {
        stopCtaShine();
        stopPatternBreathing();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (viewPager != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (currentPage > 0) {
            viewPager.setCurrentItem(currentPage - 1, true);
            return;
        }
        super.onBackPressed();
    }

    private void bindPageState(int position) {
        boolean isLastPage = position == TOTAL_SLIDE_COUNT - 1;
        segmentedProgressView.setVisibility(View.VISIBLE);
        skipTextView.setVisibility(View.VISIBLE);
        legalTextView.setVisibility(View.GONE);
        ctaButton.setText(isLastPage
                ? R.string.onboarding_cta_continue
                : R.string.onboarding_cta_next);
        segmentedProgressView.bindStaticState(position);
        maybeStartCtaShine();
    }

    private void completeOnboarding() {
        if (completingOnboarding) {
            return;
        }
        completingOnboarding = true;
        skipTextView.setEnabled(false);
        ctaButton.setEnabled(false);
        viewPager.setUserInputEnabled(false);
        stopCtaShine();
        stopPatternBreathing();
        onboardingPreferences.setCompleted(true);

        viewPager.post(() -> {
            Intent intent = AppLaunchRouter.createPostOnboardingIntent(this);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void maybeStartCtaShine() {
        if (ctaShineView == null) {
            return;
        }
        if (currentPage != TOTAL_SLIDE_COUNT - 1) {
            stopCtaShine();
            ctaShineView.setVisibility(View.GONE);
            return;
        }
        ctaShineView.setVisibility(View.VISIBLE);
        if (shineAnimator != null && shineAnimator.isRunning()) {
            return;
        }
        ViewTreeObserver observer = ctaButton.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ctaButton.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                startCtaShineAnimation();
            }
        });
    }

    private void startCtaShineAnimation() {
        stopCtaShine();
        float travelDistance = ctaButton.getWidth() + ctaShineView.getWidth();
        ctaShineView.setTranslationX(-travelDistance);
        shineAnimator = ObjectAnimator.ofFloat(ctaShineView, View.TRANSLATION_X, -travelDistance, travelDistance);
        shineAnimator.setDuration(1600L);
        shineAnimator.setStartDelay(400L);
        shineAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        shineAnimator.setRepeatMode(ObjectAnimator.RESTART);
        shineAnimator.start();
    }

    private void stopCtaShine() {
        if (shineAnimator != null) {
            shineAnimator.cancel();
            shineAnimator = null;
        }
    }

    private void maybeStartPatternBreathing() {
        if (patternView == null || patternAnimator != null) {
            return;
        }
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(patternView, View.ALPHA, 0.18f, 0.28f, 0.18f);
        alphaAnimator.setDuration(3400L);
        alphaAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        alphaAnimator.setRepeatMode(ObjectAnimator.RESTART);

        patternAnimator = new AnimatorSet();
        patternAnimator.playTogether(alphaAnimator);
        patternAnimator.start();
    }

    private void stopPatternBreathing() {
        if (patternAnimator != null) {
            patternAnimator.cancel();
            patternAnimator = null;
        }
    }
}

