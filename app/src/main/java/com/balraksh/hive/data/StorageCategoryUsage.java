package com.balraksh.hive.data;

public class StorageCategoryUsage {

    private final String label;
    private final long bytes;
    private final int accentColor;

    public StorageCategoryUsage(String label, long bytes, int accentColor) {
        this.label = label;
        this.bytes = bytes;
        this.accentColor = accentColor;
    }

    public String getLabel() {
        return label;
    }

    public long getBytes() {
        return bytes;
    }

    public int getAccentColor() {
        return accentColor;
    }
}
