package com.balraksh.hive.data;

import android.net.Uri;

public class MediaImageItem {

    private final long id;
    private final Uri uri;
    private final String displayName;
    private final long sizeBytes;
    private final long dateTaken;
    private final int width;
    private final int height;
    private final long bucketId;
    private final String bucketName;
    private final String relativePath;
    private long perceptualHash;

    public MediaImageItem(
            long id,
            Uri uri,
            String displayName,
            long sizeBytes,
            long dateTaken,
            int width,
            int height,
            long bucketId,
            String bucketName,
            String relativePath
    ) {
        this.id = id;
        this.uri = uri;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
        this.dateTaken = dateTaken;
        this.width = width;
        this.height = height;
        this.bucketId = bucketId;
        this.bucketName = bucketName;
        this.relativePath = relativePath;
        this.perceptualHash = Long.MIN_VALUE;
    }

    public long getId() {
        return id;
    }

    public Uri getUri() {
        return uri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public long getDateTaken() {
        return dateTaken;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getBucketId() {
        return bucketId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getPerceptualHash() {
        return perceptualHash;
    }

    public void setPerceptualHash(long perceptualHash) {
        this.perceptualHash = perceptualHash;
    }

    public long getPixelCount() {
        return (long) width * (long) height;
    }
}

