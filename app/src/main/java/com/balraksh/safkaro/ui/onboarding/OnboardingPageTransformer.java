package com.balraksh.safkaro.ui.onboarding;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class OnboardingPageTransformer implements ViewPager2.PageTransformer {

    @Override
    public void transformPage(@NonNull View page, float position) {
        float absPosition = Math.abs(position);
        page.setAlpha(1f - Math.min(absPosition * 0.45f, 0.45f));
        page.setTranslationX((-position) * page.getWidth() * 0.12f);
        float scale = 1f - (absPosition * 0.05f);
        page.setScaleX(scale);
        page.setScaleY(scale);
    }
}
