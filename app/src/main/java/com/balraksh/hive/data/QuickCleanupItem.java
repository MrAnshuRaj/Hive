package com.balraksh.hive.data;

public class QuickCleanupItem {

    public static final int TYPE_DUPLICATES = 1;
    public static final int TYPE_SIMILAR = 2;
    public static final int TYPE_LARGE_VIDEOS = 3;

    private final int type;
    private final String label;
    private final int count;
    private final long estimatedBytes;

    public QuickCleanupItem(int type, String label, int count, long estimatedBytes) {
        this.type = type;
        this.label = label;
        this.count = count;
        this.estimatedBytes = estimatedBytes;
    }

    public int getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public int getCount() {
        return count;
    }

    public long getEstimatedBytes() {
        return estimatedBytes;
    }
}
