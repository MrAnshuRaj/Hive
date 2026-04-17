package com.balraksh.hive.data;

public class SmartInsightItem {

    public static final int ACTION_COMPRESS_VIDEOS = 1;
    public static final int ACTION_REMOVE_DUPLICATES = 2;

    private final int actionType;
    private final int iconResId;
    private final String title;
    private final String subtitle;
    private final String ctaLabel;

    public SmartInsightItem(int actionType, int iconResId, String title, String subtitle, String ctaLabel) {
        this.actionType = actionType;
        this.iconResId = iconResId;
        this.title = title;
        this.subtitle = subtitle;
        this.ctaLabel = ctaLabel;
    }

    public int getActionType() {
        return actionType;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getCtaLabel() {
        return ctaLabel;
    }
}
