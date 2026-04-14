package com.balraksh.safkaro.ui.onboarding;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.balraksh.safkaro.R;

public class OnboardingFeatureFragment extends BaseOnboardingFragment {

    private static final String ARG_SLIDE_INDEX = "slide_index";

    public static OnboardingFeatureFragment newInstance(int slideIndex) {
        Bundle args = new Bundle();
        args.putInt(ARG_SLIDE_INDEX, slideIndex);
        OnboardingFeatureFragment fragment = new OnboardingFeatureFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutResId(), container, false);
    }

    @Override
    protected void startLoopAnimations(@NonNull View root) {
        switch (getSlideIndex()) {
            case 0:
                animateStorageFull(root);
                break;
            case 1:
                animatePhoneSlow(root);
                break;
            case 2:
                animateLargeVideos(root);
                break;
            case 3:
                animateSmartClean(root);
                break;
            case 4:
                animateSwipeOrganize(root);
                break;
            case 5:
                animateCompression(root);
                break;
            default:
                break;
        }
    }

    private int getLayoutResId() {
        switch (getSlideIndex()) {
            case 0:
                return R.layout.fragment_onboarding_storage_full;
            case 1:
                return R.layout.fragment_onboarding_phone_slow;
            case 2:
                return R.layout.fragment_onboarding_large_videos;
            case 3:
                return R.layout.fragment_onboarding_smart_clean;
            case 4:
                return R.layout.fragment_onboarding_swipe_organize;
            case 5:
            default:
                return R.layout.fragment_onboarding_lossless_compression;
        }
    }

    private int getSlideIndex() {
        Bundle args = getArguments();
        return args != null ? args.getInt(ARG_SLIDE_INDEX, 0) : 0;
    }

    private void animateStorageFull(@NonNull View root) {
        View glow = root.findViewById(R.id.viewStorageGlow);
        View fill = root.findViewById(R.id.viewStorageFill);
        View alert = root.findViewById(R.id.imageStorageAlert);
        View headline = root.findViewById(R.id.textStorageStatus);

        createPulseAnimator(glow, 0.96f, 1.08f, 1700L);
        createAlphaPulseAnimator(glow, 0.18f, 0.34f, 1700L);
        createScaleXAnimator(fill, 0.18f, 0.94f, 2300L);
        createPulseAnimator(headline, 0.98f, 1.03f, 1200L);

        ObjectAnimator shakeAnimator = ObjectAnimator.ofFloat(alert, View.ROTATION, -6f, 6f, -4f, 4f, 0f);
        shakeAnimator.setDuration(1200L);
        shakeAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        shakeAnimator.setRepeatMode(ObjectAnimator.RESTART);
        shakeAnimator.start();
        trackAnimator(shakeAnimator);
    }

    private void animatePhoneSlow(@NonNull View root) {
        View glow = root.findViewById(R.id.viewLagGlow);
        View ring = root.findViewById(R.id.imageLagRing);
        View barOne = root.findViewById(R.id.viewLagBarOne);
        View barTwo = root.findViewById(R.id.viewLagBarTwo);
        View barThree = root.findViewById(R.id.viewLagBarThree);

        createPulseAnimator(glow, 0.97f, 1.08f, 1800L);
        createAlphaPulseAnimator(glow, 0.12f, 0.26f, 1800L);
        createRotateAnimator(ring, 0f, 360f, 2600L);
        animateLoadingBar(barOne, 0L);
        animateLoadingBar(barTwo, 180L);
        animateLoadingBar(barThree, 360L);
    }

    private void animateLargeVideos(@NonNull View root) {
        View glow = root.findViewById(R.id.viewVideoGlow);
        View progress = root.findViewById(R.id.viewVideoProgressFill);
        View size = root.findViewById(R.id.textVideoSizeHighlight);
        View chip = root.findViewById(R.id.cardVideoSizeChip);

        createPulseAnimator(glow, 0.96f, 1.08f, 1750L);
        createAlphaPulseAnimator(glow, 0.14f, 0.3f, 1750L);
        createScaleXAnimator(progress, 0.2f, 0.92f, 2100L);
        createPulseAnimator(size, 0.98f, 1.05f, 1250L);
        createFloatAnimator(chip, -dp(4), dp(4), 1500L);
    }

    private void animateSmartClean(@NonNull View root) {
        View glow = root.findViewById(R.id.viewCleanGlow);
        View orbit = root.findViewById(R.id.imageSmartOrbit);
        View cardOne = root.findViewById(R.id.cardCleanOne);
        View cardTwo = root.findViewById(R.id.cardCleanTwo);
        View cardThree = root.findViewById(R.id.cardCleanThree);

        createPulseAnimator(glow, 0.96f, 1.08f, 1800L);
        createAlphaPulseAnimator(glow, 0.12f, 0.26f, 1800L);
        createRotateAnimator(orbit, 0f, 360f, 3200L);

        animateCardEntrance(cardOne, 0L);
        animateCardEntrance(cardTwo, 140L);
        animateCardEntrance(cardThree, 280L);
        createFloatAnimator(cardOne, -dp(2), dp(4), 1800L);
        createFloatAnimator(cardTwo, dp(2), -dp(3), 1600L);
        createFloatAnimator(cardThree, -dp(3), dp(3), 1700L);
    }

    private void animateSwipeOrganize(@NonNull View root) {
        View glow = root.findViewById(R.id.viewSwipeGlow);
        View backCard = root.findViewById(R.id.cardSwipeBack);
        View frontCard = root.findViewById(R.id.cardSwipeFront);
        View keepBadge = root.findViewById(R.id.cardSwipeKeep);
        View deleteBadge = root.findViewById(R.id.cardSwipeDelete);

        createPulseAnimator(glow, 0.96f, 1.08f, 1800L);
        createAlphaPulseAnimator(glow, 0.12f, 0.24f, 1800L);

        createFloatAnimator(backCard, dp(6), -dp(4), 2200L);

        keepBadge.setAlpha(0.18f);
        deleteBadge.setAlpha(0.18f);

        ObjectAnimator swipeAnimator = ObjectAnimator.ofPropertyValuesHolder(
                frontCard,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -dp(22), dp(28), -dp(10)),
                PropertyValuesHolder.ofFloat(View.ROTATION, -4f, 5f, -1.5f)
        );
        swipeAnimator.setDuration(3400L);
        swipeAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        swipeAnimator.setRepeatMode(ObjectAnimator.RESTART);
        swipeAnimator.start();
        trackAnimator(swipeAnimator);

        ObjectAnimator keepAnimator = ObjectAnimator.ofPropertyValuesHolder(
                keepBadge,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.72f, 1.08f, 0.84f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.72f, 1.08f, 0.84f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.18f, 1f, 0.26f)
        );
        keepAnimator.setDuration(3400L);
        keepAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        keepAnimator.setRepeatMode(ObjectAnimator.RESTART);
        keepAnimator.start();
        trackAnimator(keepAnimator);

        ObjectAnimator deleteAnimator = ObjectAnimator.ofPropertyValuesHolder(
                deleteBadge,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.78f, 0.84f, 1.02f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.78f, 0.84f, 1.02f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.18f, 0.18f, 0.96f)
        );
        deleteAnimator.setDuration(3400L);
        deleteAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        deleteAnimator.setRepeatMode(ObjectAnimator.RESTART);
        deleteAnimator.start();
        trackAnimator(deleteAnimator);
    }

    private void animateCompression(@NonNull View root) {
        View glow = root.findViewById(R.id.viewCompressionGlow);
        View progress = root.findViewById(R.id.viewCompressionFill);
        View arrow = root.findViewById(R.id.imageCompressionArrow);
        View oldSize = root.findViewById(R.id.textCompressionOldSize);
        View newSize = root.findViewById(R.id.textCompressionNewSize);

        createPulseAnimator(glow, 0.96f, 1.08f, 1800L);
        createAlphaPulseAnimator(glow, 0.12f, 0.26f, 1800L);
        createScaleXAnimator(progress, 0.12f, 0.95f, 2300L);
        createFloatAnimator(arrow, -dp(6), dp(6), 1000L);

        ObjectAnimator transitionAnimator = ObjectAnimator.ofPropertyValuesHolder(
                oldSize,
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.4f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.92f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.92f)
        );
        transitionAnimator.setDuration(1400L);
        transitionAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        transitionAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        transitionAnimator.start();
        trackAnimator(transitionAnimator);

        createPulseAnimator(newSize, 0.96f, 1.08f, 1400L);
    }

    private void animateLoadingBar(View bar, long delayMillis) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                bar,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.45f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.92f, 1f)
        );
        animator.setDuration(900L);
        animator.setStartDelay(delayMillis);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.start();
        trackAnimator(animator);
    }

    private void animateCardEntrance(View card, long delayMillis) {
        card.setAlpha(0f);
        card.setTranslationY(dp(24));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(card, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, dp(24), 0f)
        );
        set.setStartDelay(delayMillis);
        set.setDuration(360L);
        set.start();
        trackAnimator(set);
    }
}
