package com.balraksh.hive.data;

public class CleanupOutcome {

    private final int deletedCount;
    private final int failedCount;
    private final long freedBytes;

    public CleanupOutcome(int deletedCount, int failedCount, long freedBytes) {
        this.deletedCount = deletedCount;
        this.failedCount = failedCount;
        this.freedBytes = freedBytes;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public long getFreedBytes() {
        return freedBytes;
    }
}

