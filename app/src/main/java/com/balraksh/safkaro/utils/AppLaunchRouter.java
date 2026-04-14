package com.balraksh.safkaro.utils;

import android.content.Context;
import android.content.Intent;

import com.balraksh.safkaro.repository.OnboardingPreferences;
import com.balraksh.safkaro.ui.home.HomeActivity;
import com.balraksh.safkaro.ui.onboarding.OnboardingActivity;
import com.balraksh.safkaro.ui.permission.PermissionActivity;

public final class AppLaunchRouter {

    private AppLaunchRouter() {
    }

    public static Intent createLaunchIntent(Context context) {
        OnboardingPreferences onboardingPreferences = new OnboardingPreferences(context);
        if (!onboardingPreferences.hasCompletedOnboarding()) {
            return new Intent(context, OnboardingActivity.class);
        }
        return createPostOnboardingIntent(context);
    }

    public static Intent createPostOnboardingIntent(Context context) {
        Class<?> destination = PermissionHelper.hasRequiredPermissions(context)
                ? HomeActivity.class
                : PermissionActivity.class;
        return new Intent(context, destination);
    }
}
