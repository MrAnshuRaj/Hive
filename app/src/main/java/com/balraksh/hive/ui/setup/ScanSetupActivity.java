package com.balraksh.hive.ui.setup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import com.balraksh.hive.R;
import com.balraksh.hive.data.ScanConfig;
import com.balraksh.hive.data.ScanMode;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.BottomNavController;
import com.balraksh.hive.ui.scanning.ScanningActivity;
import com.balraksh.hive.ui.setup.folder.FolderSelectActivity;

public class ScanSetupActivity extends BaseEdgeToEdgeActivity {

    private MaterialCardView allImagesCard;
    private MaterialCardView folderCard;
    private TextView allImagesTitle;
    private TextView folderTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_scan_setup);
        BottomNavController.bind(this, BottomNavController.TAB_CLEAN);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        allImagesCard = findViewById(R.id.cardAllImages);
        folderCard = findViewById(R.id.cardSelectFolder);
        allImagesTitle = findViewById(R.id.textAllImagesTitle);
        folderTitle = findViewById(R.id.textFolderTitle);

        allImagesCard.setOnClickListener(v -> {
            bindSelectionCards();
            startScan(ScanMode.ALL_IMAGES, ScanConfig.NO_BUCKET_SELECTED, null);
        });
        folderCard.setOnClickListener(v -> openFolderSelection());
        bindSelectionCards();
    }

    private void openFolderSelection() {
        startActivity(new Intent(this, FolderSelectActivity.class));
    }

    private void bindSelectionCards() {
        bindCardState(allImagesCard, true);
        bindCardState(folderCard, false);
        allImagesTitle.setTextColor(getColor(R.color.color_scan_gold));
        folderTitle.setTextColor(getColor(R.color.color_scan_text_primary));
    }

    private void bindCardState(MaterialCardView cardView, boolean selected) {
        cardView.setStrokeWidth(selected ? getResources().getDimensionPixelSize(R.dimen.space_2) : 1);
        cardView.setStrokeColor(getColor(selected ? R.color.color_scan_gold : R.color.color_scan_border));
        cardView.setCardBackgroundColor(getColor(selected ? R.color.color_scan_surface_active : R.color.color_scan_surface_alt));
    }

    private void startScan(ScanMode scanMode, long selectedBucketId, String selectedBucketName) {
        ScanConfig config = new ScanConfig(
                scanMode,
                isDuplicateDetectionEnabled(),
                isSimilarDetectionEnabled(),
                selectedBucketId,
                selectedBucketName
        );
        Intent intent = new Intent(this, ScanningActivity.class);
        config.writeToIntent(intent);
        startActivity(intent);
    }

    private boolean isDuplicateDetectionEnabled() {
        return true;
    }

    private boolean isSimilarDetectionEnabled() {
        return true;
    }
}

