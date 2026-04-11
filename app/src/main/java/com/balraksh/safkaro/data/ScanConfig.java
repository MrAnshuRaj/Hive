package com.balraksh.safkaro.data;

import android.content.Intent;

public class ScanConfig {

    public static final long NO_BUCKET_SELECTED = Long.MIN_VALUE;

    private static final String EXTRA_SCAN_MODE = "extra_scan_mode";
    private static final String EXTRA_DUPLICATES = "extra_duplicates";
    private static final String EXTRA_SIMILAR = "extra_similar";
    private static final String EXTRA_BUCKET_ID = "extra_bucket_id";
    private static final String EXTRA_BUCKET_NAME = "extra_bucket_name";

    private final ScanMode scanMode;
    private final boolean detectDuplicates;
    private final boolean detectSimilar;
    private final long selectedBucketId;
    private final String selectedBucketName;

    public ScanConfig(
            ScanMode scanMode,
            boolean detectDuplicates,
            boolean detectSimilar,
            long selectedBucketId,
            String selectedBucketName
    ) {
        this.scanMode = scanMode;
        this.detectDuplicates = detectDuplicates;
        this.detectSimilar = detectSimilar;
        this.selectedBucketId = selectedBucketId;
        this.selectedBucketName = selectedBucketName;
    }

    public ScanMode getScanMode() {
        return scanMode;
    }

    public boolean isDetectDuplicates() {
        return detectDuplicates;
    }

    public boolean isDetectSimilar() {
        return detectSimilar;
    }

    public long getSelectedBucketId() {
        return selectedBucketId;
    }

    public String getSelectedBucketName() {
        return selectedBucketName;
    }

    public void writeToIntent(Intent intent) {
        intent.putExtra(EXTRA_SCAN_MODE, scanMode.name());
        intent.putExtra(EXTRA_DUPLICATES, detectDuplicates);
        intent.putExtra(EXTRA_SIMILAR, detectSimilar);
        intent.putExtra(EXTRA_BUCKET_ID, selectedBucketId);
        intent.putExtra(EXTRA_BUCKET_NAME, selectedBucketName);
    }

    public static ScanConfig fromIntent(Intent intent) {
        String modeName = intent.getStringExtra(EXTRA_SCAN_MODE);
        ScanMode mode = modeName == null ? ScanMode.ALL_IMAGES : ScanMode.valueOf(modeName);
        return new ScanConfig(
                mode,
                intent.getBooleanExtra(EXTRA_DUPLICATES, true),
                intent.getBooleanExtra(EXTRA_SIMILAR, true),
                intent.getLongExtra(EXTRA_BUCKET_ID, NO_BUCKET_SELECTED),
                intent.getStringExtra(EXTRA_BUCKET_NAME)
        );
    }
}
