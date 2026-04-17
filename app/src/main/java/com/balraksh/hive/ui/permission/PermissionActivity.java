package com.balraksh.hive.ui.permission;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Map;

import com.balraksh.hive.R;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.home.HomeActivity;
import com.balraksh.hive.utils.PermissionHelper;

public class PermissionActivity extends BaseEdgeToEdgeActivity {

    private ActivityResultLauncher<String[]> permissionLauncher;
    private TextView descriptionText;
    private MaterialButton allowButton;
    private boolean permanentlyDenied;
    private boolean hasRequestedPermissions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_permission);
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);

        descriptionText = findViewById(R.id.textPermissionDescription);
        allowButton = findViewById(R.id.buttonAllowAccess);
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::handlePermissionResult
        );

        allowButton.setOnClickListener(v -> {
            if (permanentlyDenied) {
                PermissionHelper.openAppSettings(this);
            } else {
                hasRequestedPermissions = true;
                permissionLauncher.launch(PermissionHelper.getRequiredPermissions());
            }
        });
        bindPermissionState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PermissionHelper.hasRequiredPermissions(this)) {
            openHome();
        } else {
            bindPermissionState();
        }
    }

    private void handlePermissionResult(Map<String, Boolean> result) {
        boolean granted = true;
        for (Boolean value : result.values()) {
            granted &= Boolean.TRUE.equals(value);
        }
        if (granted && PermissionHelper.hasRequiredPermissions(this)) {
            openHome();
            return;
        }
        Toast.makeText(this, R.string.permission_required_toast, Toast.LENGTH_SHORT).show();
        bindPermissionState();
    }

    private void bindPermissionState() {
        permanentlyDenied = hasRequestedPermissions && !PermissionHelper.shouldShowRationale(this);
        if (permanentlyDenied) {
            descriptionText.setText(R.string.permission_denied_message);
            allowButton.setText(R.string.open_settings);
        } else {
            descriptionText.setText(R.string.storage_access_message);
            allowButton.setText(R.string.allow_access);
        }
    }

    private void openHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
