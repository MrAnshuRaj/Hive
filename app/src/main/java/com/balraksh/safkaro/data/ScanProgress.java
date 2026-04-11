package com.balraksh.safkaro.data;

public class ScanProgress {

    private final int progressPercent;
    private final int scannedCount;
    private final int matchCount;
    private final String stageLabel;

    public ScanProgress(int progressPercent, int scannedCount, int matchCount, String stageLabel) {
        this.progressPercent = progressPercent;
        this.scannedCount = scannedCount;
        this.matchCount = matchCount;
        this.stageLabel = stageLabel;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public int getScannedCount() {
        return scannedCount;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public String getStageLabel() {
        return stageLabel;
    }
}
