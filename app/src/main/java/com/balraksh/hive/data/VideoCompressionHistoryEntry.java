package com.balraksh.hive.data;

public class VideoCompressionHistoryEntry {

    private final long timestampMillis;
    private final int compressedCount;
    private final long savedBytes;

    public VideoCompressionHistoryEntry(long timestampMillis, int compressedCount, long savedBytes) {
        this.timestampMillis = timestampMillis;
        this.compressedCount = compressedCount;
        this.savedBytes = savedBytes;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public int getCompressedCount() {
        return compressedCount;
    }

    public long getSavedBytes() {
        return savedBytes;
    }
}
