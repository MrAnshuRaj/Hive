package com.balraksh.safkaro.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.repository.CleanupPreferences;
import com.balraksh.safkaro.repository.ScanSessionStore;
import com.balraksh.safkaro.ui.BaseEdgeToEdgeActivity;
import com.balraksh.safkaro.ui.permission.PermissionActivity;
import com.balraksh.safkaro.ui.results.ScanResultsActivity;
import com.balraksh.safkaro.ui.setup.ScanSetupActivity;
import com.balraksh.safkaro.ui.video.VideoCompressionActivity;
import com.balraksh.safkaro.utils.FormatUtils;
import com.balraksh.safkaro.utils.PermissionHelper;

public class HomeActivity extends BaseEdgeToEdgeActivity {

    private CleanupPreferences cleanupPreferences;
    private TextView storageSavedValueText;
    private MaterialCardView quickCleanCard;
    private TextView quickCleanText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_home);
        cleanupPreferences = new CleanupPreferences(this);

        storageSavedValueText = findViewById(R.id.textStorageSavedValue);
        quickCleanCard = findViewById(R.id.cardQuickClean);
        quickCleanText = findViewById(R.id.textQuickClean);

        findViewById(R.id.cardCleanPhotos).setOnClickListener(v -> openCleanPhotos());
        findViewById(R.id.cardCompressVideos).setOnClickListener(v ->
                startActivity(new Intent(this, VideoCompressionActivity.class)));
        quickCleanCard.setOnClickListener(v -> openQuickClean());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindSummary();
    }

    private void bindSummary() {
        long totalFreedBytes = cleanupPreferences.getTotalFreedBytes();
        storageSavedValueText.setText(FormatUtils.formatStorage(totalFreedBytes));

        long quickCleanBytes = ScanSessionStore.getInstance().hasCurrentResult()
                ? ScanSessionStore.getInstance().getSelectedBytes()
                : cleanupPreferences.getLastScanPotentialBytes();
        if (quickCleanBytes > 0L) {
            quickCleanCard.setVisibility(View.VISIBLE);
            quickCleanText.setText(getString(R.string.quick_clean_available, FormatUtils.formatStorage(quickCleanBytes)));
        } else {
            quickCleanCard.setVisibility(View.GONE);
        }
    }

    private void openCleanPhotos() {
        if (!PermissionHelper.hasRequiredPermissions(this)) {
            startActivity(new Intent(this, PermissionActivity.class));
            return;
        }
        startActivity(new Intent(this, ScanSetupActivity.class));
    }

    private void openQuickClean() {
        if (ScanSessionStore.getInstance().hasCurrentResult()) {
            startActivity(new Intent(this, ScanResultsActivity.class));
        } else {
            openCleanPhotos();
        }
    }
}
