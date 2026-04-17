package com.balraksh.hive.video;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VideoCompressionResult {

    private final String sourceName;
    private final Uri sourceUri;
    private final long inputSizeBytes;
    private final long outputSizeBytes;
    private final long durationMs;
    private final int outputWidth;
    private final int outputHeight;
    private final int bitrateUsed;
    private final int fpsUsed;
    private final boolean success;
    private final String errorMessage;
    private final Uri outputUri;
    private final String outputDisplayName;

    private VideoCompressionResult(
            @NonNull String sourceName,
            @NonNull Uri sourceUri,
            long inputSizeBytes,
            long outputSizeBytes,
            long durationMs,
            int outputWidth,
            int outputHeight,
            int bitrateUsed,
            int fpsUsed,
            boolean success,
            @Nullable String errorMessage,
            @Nullable Uri outputUri,
            @Nullable String outputDisplayName
    ) {
        this.sourceName = sourceName;
        this.sourceUri = sourceUri;
        this.inputSizeBytes = inputSizeBytes;
        this.outputSizeBytes = outputSizeBytes;
        this.durationMs = durationMs;
        this.outputWidth = outputWidth;
        this.outputHeight = outputHeight;
        this.bitrateUsed = bitrateUsed;
        this.fpsUsed = fpsUsed;
        this.success = success;
        this.errorMessage = errorMessage;
        this.outputUri = outputUri;
        this.outputDisplayName = outputDisplayName;
    }

    @NonNull
    public static VideoCompressionResult success(
            @NonNull String sourceName,
            @NonNull Uri sourceUri,
            long inputSizeBytes,
            long outputSizeBytes,
            long durationMs,
            int outputWidth,
            int outputHeight,
            int bitrateUsed,
            int fpsUsed,
            @NonNull Uri outputUri,
            @NonNull String outputDisplayName
    ) {
        return new VideoCompressionResult(
                sourceName,
                sourceUri,
                inputSizeBytes,
                outputSizeBytes,
                durationMs,
                outputWidth,
                outputHeight,
                bitrateUsed,
                fpsUsed,
                true,
                null,
                outputUri,
                outputDisplayName
        );
    }

    @NonNull
    public static VideoCompressionResult failure(
            @NonNull String sourceName,
            @NonNull Uri sourceUri,
            long inputSizeBytes,
            long durationMs,
            @NonNull String errorMessage
    ) {
        return new VideoCompressionResult(
                sourceName,
                sourceUri,
                inputSizeBytes,
                0L,
                durationMs,
                0,
                0,
                0,
                0,
                false,
                errorMessage,
                null,
                null
        );
    }

    @NonNull
    public String getSourceName() {
        return sourceName;
    }

    @NonNull
    public Uri getSourceUri() {
        return sourceUri;
    }

    public long getInputSizeBytes() {
        return inputSizeBytes;
    }

    public long getOutputSizeBytes() {
        return outputSizeBytes;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getOutputWidth() {
        return outputWidth;
    }

    public int getOutputHeight() {
        return outputHeight;
    }

    public int getBitrateUsed() {
        return bitrateUsed;
    }

    public int getFpsUsed() {
        return fpsUsed;
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Nullable
    public Uri getOutputUri() {
        return outputUri;
    }

    @Nullable
    public String getOutputDisplayName() {
        return outputDisplayName;
    }

    public long getSavedBytes() {
        return Math.max(0L, inputSizeBytes - outputSizeBytes);
    }

    public int getSavedPercent() {
        if (!success || inputSizeBytes <= 0L) {
            return 0;
        }
        return (int) ((getSavedBytes() * 100L) / inputSizeBytes);
    }
}

