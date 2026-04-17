package com.balraksh.hive.video;

import androidx.annotation.NonNull;

public class VideoCompressionProgress {

    private final int currentIndex;
    private final int totalCount;
    private final int completedCount;
    private final int failedCount;
    private final int itemProgressPercent;
    private final int overallProgressPercent;
    private final long estimatedRemainingMs;
    private final String currentFileName;
    private final VideoCompressionStage stage;

    public VideoCompressionProgress(
            int currentIndex,
            int totalCount,
            int completedCount,
            int failedCount,
            int itemProgressPercent,
            int overallProgressPercent,
            long estimatedRemainingMs,
            @NonNull String currentFileName,
            @NonNull VideoCompressionStage stage
    ) {
        this.currentIndex = currentIndex;
        this.totalCount = totalCount;
        this.completedCount = completedCount;
        this.failedCount = failedCount;
        this.itemProgressPercent = itemProgressPercent;
        this.overallProgressPercent = overallProgressPercent;
        this.estimatedRemainingMs = estimatedRemainingMs;
        this.currentFileName = currentFileName;
        this.stage = stage;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getItemProgressPercent() {
        return itemProgressPercent;
    }

    public int getOverallProgressPercent() {
        return overallProgressPercent;
    }

    public long getEstimatedRemainingMs() {
        return estimatedRemainingMs;
    }

    @NonNull
    public String getCurrentFileName() {
        return currentFileName;
    }

    @NonNull
    public VideoCompressionStage getStage() {
        return stage;
    }
}

