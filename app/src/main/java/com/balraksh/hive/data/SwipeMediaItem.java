package com.balraksh.hive.data;

import android.net.Uri;

import androidx.annotation.NonNull;

public class SwipeMediaItem {

    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_VIDEO = 1;

    private final long id;
    private final int type;
    private final String stableKey;
    private final Uri uri;
    private final String displayName;
    private final long sizeBytes;
    private final long dateMillis;
    private final int width;
    private final int height;
    private final long durationMs;
    private final long bucketId;
    private final String bucketName;
    private final String relativePath;
    private boolean bestShot;

    public SwipeMediaItem(
            long id,
            int type,
            @NonNull Uri uri,
            @NonNull String displayName,
            long sizeBytes,
            long dateMillis,
            int width,
            int height,
            long durationMs,
            long bucketId,
            @NonNull String bucketName,
            @NonNull String relativePath
    ) {
        this.id = id;
        this.type = type;
        this.stableKey = (type == TYPE_VIDEO ? "video_" : "image_") + id;
        this.uri = uri;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
        this.dateMillis = dateMillis;
        this.width = width;
        this.height = height;
        this.durationMs = durationMs;
        this.bucketId = bucketId;
        this.bucketName = bucketName;
        this.relativePath = relativePath;
    }

    public long getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    @NonNull
    public String getStableKey() {
        return stableKey;
    }

    @NonNull
    public Uri getUri() {
        return uri;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public long getBucketId() {
        return bucketId;
    }

    @NonNull
    public String getBucketName() {
        return bucketName;
    }

    @NonNull
    public String getRelativePath() {
        return relativePath;
    }

    public boolean isBestShot() {
        return bestShot;
    }

    public void setBestShot(boolean bestShot) {
        this.bestShot = bestShot;
    }

    public boolean isVideo() {
        return type == TYPE_VIDEO;
    }

    public long getPixelCount() {
        return (long) width * (long) height;
    }
}
