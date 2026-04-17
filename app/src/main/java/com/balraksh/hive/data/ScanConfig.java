package com.balraksh.hive.data;

import android.content.Intent;

public class ScanConfig {

    public static final long NO_BUCKET_SELECTED = Long.MIN_VALUE;

    private static final String EXTRA_SCAN_MODE = "extra_scan_mode";
    private static final String EXTRA_DUPLICATES = "extra_duplicates";
    private static final String EXTRA_SIMILAR = "extra_similar";
    private static final String EXTRA_BUCKET_ID = "extra_bucket_id";
    private static final String EXTRA_BUCKET_NAME = "extra_bucket_name";
    private static final String EXTRA_BUCKET_IDS = "extra_bucket_ids";
    private static final String EXTRA_BUCKET_NAMES = "extra_bucket_names";

    private final ScanMode scanMode;
    private final boolean detectDuplicates;
    private final boolean detectSimilar;
    private final long selectedBucketId;
    private final String selectedBucketName;
    private final long[] selectedBucketIds;
    private final String[] selectedBucketNames;

    public ScanConfig(
            ScanMode scanMode,
            boolean detectDuplicates,
            boolean detectSimilar,
            long selectedBucketId,
            String selectedBucketName
    ) {
        this(scanMode, detectDuplicates, detectSimilar, selectedBucketId, selectedBucketName, null, null);
    }

    public ScanConfig(
            ScanMode scanMode,
            boolean detectDuplicates,
            boolean detectSimilar,
            long selectedBucketId,
            String selectedBucketName,
            long[] selectedBucketIds,
            String[] selectedBucketNames
    ) {
        this.scanMode = scanMode;
        this.detectDuplicates = detectDuplicates;
        this.detectSimilar = detectSimilar;
        this.selectedBucketId = selectedBucketId;
        this.selectedBucketName = selectedBucketName;
        this.selectedBucketIds = selectedBucketIds;
        this.selectedBucketNames = selectedBucketNames;
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

    public long[] getSelectedBucketIds() {
        return selectedBucketIds;
    }

    public String[] getSelectedBucketNames() {
        return selectedBucketNames;
    }

    public void writeToIntent(Intent intent) {
        intent.putExtra(EXTRA_SCAN_MODE, scanMode.name());
        intent.putExtra(EXTRA_DUPLICATES, detectDuplicates);
        intent.putExtra(EXTRA_SIMILAR, detectSimilar);
        intent.putExtra(EXTRA_BUCKET_ID, selectedBucketId);
        intent.putExtra(EXTRA_BUCKET_NAME, selectedBucketName);
        intent.putExtra(EXTRA_BUCKET_IDS, selectedBucketIds);
        intent.putExtra(EXTRA_BUCKET_NAMES, selectedBucketNames);
    }

    public static ScanConfig fromIntent(Intent intent) {
        String modeName = intent.getStringExtra(EXTRA_SCAN_MODE);
        ScanMode mode = modeName == null ? ScanMode.ALL_IMAGES : ScanMode.valueOf(modeName);
        long[] bucketIds = intent.getLongArrayExtra(EXTRA_BUCKET_IDS);
        String[] bucketNames = intent.getStringArrayExtra(EXTRA_BUCKET_NAMES);
        long selectedBucketId = intent.getLongExtra(EXTRA_BUCKET_ID, NO_BUCKET_SELECTED);
        String selectedBucketName = intent.getStringExtra(EXTRA_BUCKET_NAME);
        if ((bucketIds == null || bucketIds.length == 0) && selectedBucketId != NO_BUCKET_SELECTED) {
            bucketIds = new long[]{selectedBucketId};
        }
        if ((bucketNames == null || bucketNames.length == 0) && selectedBucketName != null) {
            bucketNames = new String[]{selectedBucketName};
        }
        return new ScanConfig(
                mode,
                intent.getBooleanExtra(EXTRA_DUPLICATES, true),
                intent.getBooleanExtra(EXTRA_SIMILAR, true),
                selectedBucketId,
                selectedBucketName,
                bucketIds,
                bucketNames
        );
    }
}

