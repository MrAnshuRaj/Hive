package com.balraksh.hive.data;

import java.util.ArrayList;
import java.util.List;

public class HomeDashboardData {

    private final long totalStorageBytes;
    private final long usedStorageBytes;
    private final long recommendedFreeBytes;
    private final List<StorageCategoryUsage> categories;
    private final WeeklySummary weeklySummary;
    private final List<QuickCleanupItem> quickCleanupItems;
    private final List<SmartInsightItem> smartInsights;

    public HomeDashboardData(
            long totalStorageBytes,
            long usedStorageBytes,
            long recommendedFreeBytes,
            List<StorageCategoryUsage> categories,
            WeeklySummary weeklySummary,
            List<QuickCleanupItem> quickCleanupItems,
            List<SmartInsightItem> smartInsights
    ) {
        this.totalStorageBytes = totalStorageBytes;
        this.usedStorageBytes = usedStorageBytes;
        this.recommendedFreeBytes = recommendedFreeBytes;
        this.categories = new ArrayList<>(categories);
        this.weeklySummary = weeklySummary;
        this.quickCleanupItems = new ArrayList<>(quickCleanupItems);
        this.smartInsights = new ArrayList<>(smartInsights);
    }

    public long getTotalStorageBytes() {
        return totalStorageBytes;
    }

    public long getUsedStorageBytes() {
        return usedStorageBytes;
    }

    public long getRecommendedFreeBytes() {
        return recommendedFreeBytes;
    }

    public List<StorageCategoryUsage> getCategories() {
        return new ArrayList<>(categories);
    }

    public WeeklySummary getWeeklySummary() {
        return weeklySummary;
    }

    public List<QuickCleanupItem> getQuickCleanupItems() {
        return new ArrayList<>(quickCleanupItems);
    }

    public List<SmartInsightItem> getSmartInsights() {
        return new ArrayList<>(smartInsights);
    }
}
