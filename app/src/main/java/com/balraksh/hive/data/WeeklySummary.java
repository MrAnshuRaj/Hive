package com.balraksh.hive.data;

public class WeeklySummary {

    private final long spaceFreedBytes;
    private final int filesOrganized;
    private final int videosCompressed;

    public WeeklySummary(long spaceFreedBytes, int filesOrganized, int videosCompressed) {
        this.spaceFreedBytes = spaceFreedBytes;
        this.filesOrganized = filesOrganized;
        this.videosCompressed = videosCompressed;
    }

    public long getSpaceFreedBytes() {
        return spaceFreedBytes;
    }

    public int getFilesOrganized() {
        return filesOrganized;
    }

    public int getVideosCompressed() {
        return videosCompressed;
    }
}
