package com.balraksh.safkaro.data;

public class BucketOption {

    private final long bucketId;
    private final String bucketName;
    private final int itemCount;

    public BucketOption(long bucketId, String bucketName, int itemCount) {
        this.bucketId = bucketId;
        this.bucketName = bucketName;
        this.itemCount = itemCount;
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

    @Override
    public String toString() {
        return bucketName + " (" + itemCount + ")";
    }
}
