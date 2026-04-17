package com.balraksh.hive.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsControllerCompat;

import com.balraksh.hive.R;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;

public class SettingsActivity extends BaseEdgeToEdgeActivity {

    public static final String EXTRA_OPEN_SECTION = "extra_open_section";
    public static final String SECTION_PRIVACY = "privacy";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_settings);
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);

        findViewById(R.id.buttonSettingsBack).setOnClickListener(v -> finish());
        findViewById(R.id.rowSettingsNotifications).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        });
        findViewById(R.id.rowSettingsPrivacy).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        findViewById(R.id.rowSettingsHelp).setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@safkaro.app"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.home_support_subject));
            startActivity(Intent.createChooser(emailIntent, getString(R.string.home_help_support)));
        });

        if (SECTION_PRIVACY.equals(getIntent().getStringExtra(EXTRA_OPEN_SECTION))) {
            findViewById(R.id.rowSettingsPrivacy).requestFocus();
        }
    }
}
