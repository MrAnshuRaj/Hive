package com.balraksh.safkaro.data;

import java.util.ArrayList;
import java.util.List;

public class ScanResult {

    private final List<DuplicateGroup> duplicateGroups;
    private final List<SimilarGroup> similarGroups;
    private final int scannedCount;
    private final int duplicateMatchCount;
    private final int similarMatchCount;
    private final long potentialSpaceBytes;
    private final long completedAtMillis;

    public ScanResult(
            List<DuplicateGroup> duplicateGroups,
            List<SimilarGroup> similarGroups,
            int scannedCount,
            int duplicateMatchCount,
            int similarMatchCount,
            long potentialSpaceBytes,
            long completedAtMillis
    ) {
        this.duplicateGroups = new ArrayList<>(duplicateGroups);
        this.similarGroups = new ArrayList<>(similarGroups);
        this.scannedCount = scannedCount;
        this.duplicateMatchCount = duplicateMatchCount;
        this.similarMatchCount = similarMatchCount;
        this.potentialSpaceBytes = potentialSpaceBytes;
        this.completedAtMillis = completedAtMillis;
    }

    public List<DuplicateGroup> getDuplicateGroups() {
        return new ArrayList<>(duplicateGroups);
    }

    public List<SimilarGroup> getSimilarGroups() {
        return new ArrayList<>(similarGroups);
    }

    public int getScannedCount() {
        return scannedCount;
    }

    public int getDuplicateMatchCount() {
        return duplicateMatchCount;
    }

    public int getSimilarMatchCount() {
        return similarMatchCount;
    }

    public int getTotalMatchCount() {
        return duplicateMatchCount + similarMatchCount;
    }

    public long getPotentialSpaceBytes() {
        return potentialSpaceBytes;
    }

    public long getCompletedAtMillis() {
        return completedAtMillis;
    }

    public boolean hasResults() {
        return !duplicateGroups.isEmpty() || !similarGroups.isEmpty();
    }
}
