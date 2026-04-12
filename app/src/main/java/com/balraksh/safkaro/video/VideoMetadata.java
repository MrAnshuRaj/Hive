package com.balraksh.safkaro.video;

import android.net.Uri;

import androidx.annotation.NonNull;

public class VideoMetadata {

    private final Uri sourceUri;
    private final String displayName;
    private final long sizeBytes;
    private final long durationMs;
    private final int width;
    private final int height;
    private final int rotationDegrees;
    private final int bitrate;
    private final float frameRate;

    public VideoMetadata(
            @NonNull Uri sourceUri,
            @NonNull String displayName,
            long sizeBytes,
            long durationMs,
            int width,
            int height,
            int rotationDegrees,
            int bitrate,
            float frameRate
    ) {
        this.sourceUri = sourceUri;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
        this.durationMs = durationMs;
        this.width = width;
        this.height = height;
        this.rotationDegrees = rotationDegrees;
        this.bitrate = bitrate;
        this.frameRate = frameRate;
    }

    @NonNull
    public Uri getSourceUri() {
        return sourceUri;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRotationDegrees() {
        return rotationDegrees;
    }

    public int getBitrate() {
        return bitrate;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public int getDisplayWidth() {
        return isRotated() ? height : width;
    }

    public int getDisplayHeight() {
        return isRotated() ? width : height;
    }

    public boolean isRotated() {
        return rotationDegrees == 90 || rotationDegrees == 270;
    }
}
