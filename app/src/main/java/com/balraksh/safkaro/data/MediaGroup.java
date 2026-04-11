package com.balraksh.safkaro.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class MediaGroup {

    private final String groupId;
    private final List<MediaImageItem> items;
    private final long bestItemId;

    protected MediaGroup(String groupId, List<MediaImageItem> items, long bestItemId) {
        this.groupId = groupId;
        this.items = new ArrayList<>(items);
        this.bestItemId = bestItemId;
    }

    public String getGroupId() {
        return groupId;
    }

    public List<MediaImageItem> getItems() {
        return new ArrayList<>(items);
    }

    public long getBestItemId() {
        return bestItemId;
    }

    public MediaImageItem getBestItem() {
        for (MediaImageItem item : items) {
            if (item.getId() == bestItemId) {
                return item;
            }
        }
        return items.isEmpty() ? null : items.get(0);
    }

    public long getSpaceToSave(Set<Long> selectedIds) {
        long total = 0L;
        for (MediaImageItem item : items) {
            if (selectedIds.contains(item.getId())) {
                total += item.getSizeBytes();
            }
        }
        return total;
    }
}
