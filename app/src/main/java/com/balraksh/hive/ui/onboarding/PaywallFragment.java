package com.balraksh.hive.ui.onboarding;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import com.balraksh.hive.R;

public class PaywallFragment extends BaseOnboardingFragment {

    public enum PlanType {
        YEARLY,
        MONTHLY
    }

    private MaterialCardView yearlyCard;
    private MaterialCardView monthlyCard;
    private TextView yearlyChip;
    private PlanType selectedPlan = PlanType.YEARLY;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_paywall, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        yearlyCard = view.findViewById(R.id.cardPlanYearly);
        monthlyCard = view.findViewById(R.id.cardPlanMonthly);
        yearlyChip = view.findViewById(R.id.textPlanYearlyBadge);

        yearlyCard.setOnClickListener(v -> updatePlanSelection(PlanType.YEARLY, true));
        monthlyCard.setOnClickListener(v -> updatePlanSelection(PlanType.MONTHLY, true));
        updatePlanSelection(selectedPlan, false);
    }

    @Override
    protected void startLoopAnimations(@NonNull View root) {
        View badge = root.findViewById(R.id.cardPaywallBadge);
        View features = root.findViewById(R.id.layoutPaywallFeatures);
        View plans = root.findViewById(R.id.layoutPlanCards);

        badge.setAlpha(0f);
        badge.setTranslationY(dp(18));
        features.setAlpha(0f);
        features.setTranslationY(dp(22));
        plans.setAlpha(0f);
        plans.setTranslationY(dp(26));

        AnimatorSet revealAnimator = new AnimatorSet();
        revealAnimator.playTogether(
                ObjectAnimator.ofFloat(badge, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(badge, View.TRANSLATION_Y, dp(18), 0f),
                ObjectAnimator.ofFloat(features, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(features, View.TRANSLATION_Y, dp(22), 0f),
                ObjectAnimator.ofFloat(plans, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(plans, View.TRANSLATION_Y, dp(26), 0f)
        );
        revealAnimator.setStartDelay(100L);
        revealAnimator.setDuration(420L);
        revealAnimator.start();
        trackAnimator(revealAnimator);

        createPulseAnimator(badge, 0.98f, 1.04f, 1600L);
        createFloatAnimator(yearlyCard, -dp(3), dp(4), 2100L);
        createFloatAnimator(monthlyCard, dp(3), -dp(2), 1900L);
        createAlphaPulseAnimator(yearlyChip, 0.7f, 1f, 1250L);
    }

    public PlanType getSelectedPlanType() {
        return selectedPlan;
    }

    private void updatePlanSelection(PlanType planType, boolean animate) {
        selectedPlan = planType;
        bindCardState(yearlyCard, yearlyChip, planType == PlanType.YEARLY, animate);
        bindCardState(monthlyCard, null, planType == PlanType.MONTHLY, animate);
    }

    private void bindCardState(MaterialCardView card, @Nullable TextView badge, boolean selected, boolean animate) {
        if (card == null) {
            return;
        }
        int backgroundColor = requireContext().getColor(selected
                ? R.color.color_onboarding_glass_strong
                : R.color.color_onboarding_glass);
        int strokeColor = requireContext().getColor(selected
                ? R.color.color_onboarding_orange
                : R.color.color_onboarding_glass_strong);

        card.setCardBackgroundColor(backgroundColor);
        card.setStrokeColor(strokeColor);
        card.setCardElevation(selected ? dp(12) : 0f);
        if (badge != null) {
            badge.setAlpha(selected ? 1f : 0.4f);
        }

        float targetScale = selected ? 1.02f : 1f;
        if (animate) {
            ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                    card,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, card.getScaleX(), targetScale),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, card.getScaleY(), targetScale)
            );
            animator.setDuration(220L);
            animator.start();
            trackAnimator(animator);
        } else {
            card.setScaleX(targetScale);
            card.setScaleY(targetScale);
        }
    }
}

