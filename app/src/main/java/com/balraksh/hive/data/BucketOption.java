package com.balraksh.hive.data;

public class BucketOption {

    private final long bucketId;
    private final String bucketName;
    private final int itemCount;
    private final long totalBytes;

    public BucketOption(long bucketId, String bucketName, int itemCount) {
        this(bucketId, bucketName, itemCount, 0L);
    }

    public BucketOption(long bucketId, String bucketName, int itemCount, long totalBytes) {
        this.bucketId = bucketId;
        this.bucketName = bucketName;
        this.itemCount = itemCount;
        this.totalBytes = totalBytes;
    }

    public long getBucketId() {
        return bucketId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public int getItemCount() {
        return itemCount;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    @Override
    public String toString() {
        return bucketName + " (" + itemCount + ")";
    }
}

