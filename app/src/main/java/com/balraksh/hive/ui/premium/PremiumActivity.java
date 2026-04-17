package com.balraksh.hive.ui.premium;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsControllerCompat;

import com.balraksh.hive.R;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.onboarding.OnboardingActivity;

public class PremiumActivity extends BaseEdgeToEdgeActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_premium);
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);

        findViewById(R.id.buttonPremiumBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonExplorePlans).setOnClickListener(v ->
                startActivity(new Intent(this, OnboardingActivity.class)));
    }
}
