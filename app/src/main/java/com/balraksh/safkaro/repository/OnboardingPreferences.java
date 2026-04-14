package com.balraksh.safkaro.repository;

import android.content.Context;
import android.content.SharedPreferences;

public class OnboardingPreferences {

    public static final String KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding";
    private static final String PREFS_NAME = "safkaro_onboarding_prefs";

    private final SharedPreferences preferences;

    public OnboardingPreferences(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasCompletedOnboarding() {
        return preferences.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false);
    }

    public void setCompleted(boolean completed) {
        preferences.edit()
                .putBoolean(KEY_HAS_COMPLETED_ONBOARDING, completed)
                .apply();
    }
}
