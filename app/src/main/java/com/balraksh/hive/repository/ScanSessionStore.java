package com.balraksh.hive.repository;

import com.balraksh.hive.data.CleanupOutcome;
import com.balraksh.hive.data.MediaGroup;
import com.balraksh.hive.data.MediaImageItem;
import com.balraksh.hive.data.ScanConfig;
import com.balraksh.hive.data.ScanResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScanSessionStore {

    private static final ScanSessionStore INSTANCE = new ScanSessionStore();

    private ScanResult currentResult;
    private ScanConfig currentConfig;
    private final Map<Long, MediaImageItem> itemsById = new LinkedHashMap<>();
    private final LinkedHashSet<Long> selectedIds = new LinkedHashSet<>();
    private CleanupOutcome lastOutcome;

    public static ScanSessionStore getInstance() {
        return INSTANCE;
    }

    public synchronized void setCurrentResult(ScanConfig config, ScanResult result) {
        currentConfig = config;
        currentResult = result;
        itemsById.clear();
        selectedIds.clear();
        indexItems(result.getDuplicateGroups());
        indexItems(result.getSimilarGroups());
    }

    private void indexItems(Collection<? extends MediaGroup> groups) {
        for (MediaGroup group : groups) {
            for (MediaImageItem item : group.getItems()) {
                itemsById.put(item.getId(), item);
                if (item.getId() != group.getBestItemId()) {
                    selectedIds.add(item.getId());
                }
            }
        }
    }

    public synchronized ScanResult getCurrentResult() {
        return currentResult;
    }

    public synchronized ScanConfig getCurrentConfig() {
        return currentConfig;
    }

    public synchronized boolean hasCurrentResult() {
        return currentResult != null;
    }

    public synchronized Set<Long> getSelectedIds() {
        return new LinkedHashSet<>(selectedIds);
    }

    public synchronized boolean isSelected(long itemId) {
        return selectedIds.contains(itemId);
    }

    public synchronized void setSelected(long itemId, boolean selected) {
        if (selected) {
            selectedIds.add(itemId);
        } else {
            selectedIds.remove(itemId);
        }
    }

    public synchronized void setSelectedForGroups(Collection<? extends MediaGroup> groups, boolean selected) {
        for (MediaGroup group : groups) {
            for (MediaImageItem item : group.getItems()) {
                if (item.getId() == group.getBestItemId()) {
                    continue;
                }
                if (selected) {
                    selectedIds.add(item.getId());
                } else {
                    selectedIds.remove(item.getId());
                }
            }
        }
    }

    public synchronized boolean hasSelectedItemsInGroups(Collection<? extends MediaGroup> groups) {
        for (MediaGroup group : groups) {
            for (MediaImageItem item : group.getItems()) {
                if (item.getId() != group.getBestItemId() && selectedIds.contains(item.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized boolean hasUnselectedItemsInGroups(Collection<? extends MediaGroup> groups) {
        for (MediaGroup group : groups) {
            for (MediaImageItem item : group.getItems()) {
                if (item.getId() != group.getBestItemId() && !selectedIds.contains(item.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized int getSelectedCount() {
        return selectedIds.size();
    }

    public synchronized long getSelectedBytes() {
        long total = 0L;
        for (Long id : selectedIds) {
            MediaImageItem item = itemsById.get(id);
            if (item != null) {
                total += item.getSizeBytes();
            }
        }
        return total;
    }

    public synchronized List<MediaImageItem> getSelectedItems() {
        List<MediaImageItem> items = new ArrayList<>();
        for (Long id : selectedIds) {
            MediaImageItem item = itemsById.get(id);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public synchronized long getGroupSelectedBytes(MediaGroup group) {
        return group.getSpaceToSave(selectedIds);
    }

    public synchronized void setLastOutcome(CleanupOutcome outcome) {
        lastOutcome = outcome;
    }

    public synchronized CleanupOutcome getLastOutcome() {
        return lastOutcome;
    }

    public synchronized void clearCurrentResult() {
        currentResult = null;
        currentConfig = null;
        itemsById.clear();
        selectedIds.clear();
    }
}

