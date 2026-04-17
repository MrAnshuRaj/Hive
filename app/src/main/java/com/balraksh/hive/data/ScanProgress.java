package com.balraksh.hive.data;

public class ScanProgress {

    private final int progressPercent;
    private final int scannedCount;
    private final int duplicateMatchCount;
    private final int similarMatchCount;
    private final String stageLabel;

    public ScanProgress(
            int progressPercent,
            int scannedCount,
            int duplicateMatchCount,
            int similarMatchCount,
            String stageLabel
    ) {
        this.progressPercent = progressPercent;
        this.scannedCount = scannedCount;
        this.duplicateMatchCount = duplicateMatchCount;
        this.similarMatchCount = similarMatchCount;
        this.stageLabel = stageLabel;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public int getScannedCount() {
        return scannedCount;
    }

    public int getDuplicateMatchCount() {
        return duplicateMatchCount;
    }

    public int getSimilarMatchCount() {
        return similarMatchCount;
    }

    public int getMatchCount() {
        return duplicateMatchCount + similarMatchCount;
    }

    public String getStageLabel() {
        return stageLabel;
    }
}

