package com.balraksh.safkaro.data;

public class CleanupHistoryEntry {

    private final long timestampMillis;
    private final int deletedCount;
    private final long freedBytes;

    public CleanupHistoryEntry(long timestampMillis, int deletedCount, long freedBytes) {
        this.timestampMillis = timestampMillis;
        this.deletedCount = deletedCount;
        this.freedBytes = freedBytes;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public long getFreedBytes() {
        return freedBytes;
    }
}
