package com.balraksh.hive.utils;

import android.content.Context;
import android.content.Intent;

import com.balraksh.hive.repository.OnboardingPreferences;
import com.balraksh.hive.ui.home.HomeActivity;
import com.balraksh.hive.ui.onboarding.OnboardingActivity;
import com.balraksh.hive.ui.permission.PermissionActivity;

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

