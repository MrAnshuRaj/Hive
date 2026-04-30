package com.balraksh.hive.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

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
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.home_help_support_url)));
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException exception) {
                Toast.makeText(this, R.string.home_opens_in_web, Toast.LENGTH_SHORT).show();
            }
        });

        if (SECTION_PRIVACY.equals(getIntent().getStringExtra(EXTRA_OPEN_SECTION))) {
            findViewById(R.id.rowSettingsPrivacy).requestFocus();
        }
    }
}
