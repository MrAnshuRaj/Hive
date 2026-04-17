package com.balraksh.hive.repository;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.balraksh.hive.R;
import com.balraksh.hive.data.BucketOption;
import com.balraksh.hive.data.DuplicateGroup;
import com.balraksh.hive.data.MediaGroup;
import com.balraksh.hive.data.MediaImageItem;
import com.balraksh.hive.data.ScanConfig;
import com.balraksh.hive.data.ScanMode;
import com.balraksh.hive.data.ScanProgress;
import com.balraksh.hive.data.ScanResult;
import com.balraksh.hive.data.SimilarGroup;
import com.balraksh.hive.utils.ImageHashUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MediaRepository {

    public interface ProgressListener {
        void onProgress(ScanProgress progress);
    }

    private static final int SIMILARITY_THRESHOLD = 10;
    private static final int PROGRESS_QUERY_END = 25;
    private static final int PROGRESS_DUPLICATE_END = 65;

    private final Context appContext;
    private final ContentResolver resolver;

    public MediaRepository(Context context) {
        appContext = context.getApplicationContext();
        resolver = appContext.getContentResolver();
    }

    public List<BucketOption> loadBuckets() {
        Map<Long, BucketOption> buckets = new LinkedHashMap<>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media._ID
        };
        try (Cursor cursor = resolver.query(
                collection,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor == null) {
                return Collections.emptyList();
            }
            int bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
            int bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            while (cursor.moveToNext()) {
                long bucketId = cursor.getLong(bucketIdIndex);
                String bucketName = cursor.getString(bucketNameIndex);
                long size = Math.max(0L, cursor.getLong(sizeIndex));
                if (TextUtils.isEmpty(bucketName)) {
                    bucketName = "Unknown";
                }
                BucketOption current = buckets.get(bucketId);
                if (current == null) {
                    buckets.put(bucketId, new BucketOption(bucketId, bucketName, 1, size));
                } else {
                    buckets.put(bucketId, new BucketOption(
                            bucketId,
                            current.getBucketName(),
                            current.getItemCount() + 1,
                            current.getTotalBytes() + size
                    ));
                }
            }
        }
        List<BucketOption> result = new ArrayList<>(buckets.values());
        result.sort((first, second) -> Integer.compare(second.getItemCount(), first.getItemCount()));
        return result;
    }

    public ScanResult scanImages(ScanConfig config, ProgressListener listener) throws IOException {
        List<MediaImageItem> images = queryImages(config, listener);
        int scannedCount = images.size();
        List<DuplicateGroup> duplicateGroups = config.isDetectDuplicates()
                ? findDuplicateGroups(images, scannedCount, listener)
                : new ArrayList<>();

        Set<Long> duplicateMemberIds = new HashSet<>();
        for (DuplicateGroup group : duplicateGroups) {
            for (MediaImageItem item : group.getItems()) {
                duplicateMemberIds.add(item.getId());
            }
        }

        int duplicateMatches = countMatches(duplicateGroups);
        List<SimilarGroup> similarGroups = config.isDetectSimilar()
                ? findSimilarGroups(images, duplicateMemberIds, scannedCount, duplicateMatches, listener)
                : new ArrayList<>();

        int similarMatches = countMatches(similarGroups);
        dispatchProgress(listener, 90, scannedCount, duplicateMatches, similarMatches,
                appContext.getString(R.string.scan_stage_finishing));

        long potentialBytes = calculatePotentialSpace(duplicateGroups, similarGroups);
        dispatchProgress(listener, 100, scannedCount, duplicateMatches, similarMatches,
                appContext.getString(R.string.scan_stage_finishing));
        return new ScanResult(
                duplicateGroups,
                similarGroups,
                scannedCount,
                duplicateMatches,
                similarMatches,
                potentialBytes,
                System.currentTimeMillis()
        );
    }

    private List<MediaImageItem> queryImages(ScanConfig config, ProgressListener listener) {
        List<MediaImageItem> images = new ArrayList<>();
        Set<Long> selectedBucketIds = resolveSelectedBucketIds(config);
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String pathColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? MediaStore.Images.Media.RELATIVE_PATH
                : MediaStore.Images.Media.DATA;
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                pathColumn
        };

        try (Cursor cursor = resolver.query(
                collection,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor == null) {
                return images;
            }

            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            int dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
            int dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
            int widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH);
            int heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT);
            int bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
            int bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int pathIndex = cursor.getColumnIndexOrThrow(pathColumn);

            int processedRows = 0;
            while (cursor.moveToNext()) {
                processedRows++;
                long id = cursor.getLong(idIndex);
                String displayName = cursor.getString(nameIndex);
                long size = cursor.getLong(sizeIndex);
                long dateTaken = cursor.getLong(dateTakenIndex);
                long dateAdded = cursor.getLong(dateAddedIndex);
                int width = cursor.getInt(widthIndex);
                int height = cursor.getInt(heightIndex);
                long bucketId = cursor.getLong(bucketIdIndex);
                String bucketName = cursor.getString(bucketNameIndex);
                String relativePath = cursor.getString(pathIndex);

                if (size <= 0) {
                    continue;
                }
                if (config.getScanMode() == ScanMode.SELECTED_BUCKET && !selectedBucketIds.contains(bucketId)) {
                    continue;
                }

                Uri uri = ContentUris.withAppendedId(collection, id);
                long timestamp = dateTaken > 0 ? dateTaken : dateAdded * 1000L;
                images.add(new MediaImageItem(
                        id,
                        uri,
                        displayName,
                        size,
                        timestamp,
                        width,
                        height,
                        bucketId,
                        bucketName,
                        relativePath
                ));

                if (processedRows % 30 == 0) {
                    int progress = Math.min(PROGRESS_QUERY_END, 5 + (processedRows / 20));
                    dispatchProgress(listener, progress, images.size(), 0, 0,
                            appContext.getString(R.string.scan_stage_loading));
                }
            }
        }
        dispatchProgress(listener, PROGRESS_QUERY_END, images.size(), 0, 0,
                appContext.getString(R.string.scan_stage_loading));
        return images;
    }

    private Set<Long> resolveSelectedBucketIds(ScanConfig config) {
        Set<Long> selectedBucketIds = new LinkedHashSet<>();
        if (config.getSelectedBucketIds() != null) {
            for (long bucketId : config.getSelectedBucketIds()) {
                if (bucketId != ScanConfig.NO_BUCKET_SELECTED) {
                    selectedBucketIds.add(bucketId);
                }
            }
        }
        if (selectedBucketIds.isEmpty() && config.getSelectedBucketId() != ScanConfig.NO_BUCKET_SELECTED) {
            selectedBucketIds.add(config.getSelectedBucketId());
        }
        return selectedBucketIds;
    }

    private List<DuplicateGroup> findDuplicateGroups(
            List<MediaImageItem> images,
            int scannedCount,
            ProgressListener listener
    ) throws IOException {
        Map<Long, List<MediaImageItem>> bySize = new LinkedHashMap<>();
        for (MediaImageItem item : images) {
            bySize.computeIfAbsent(item.getSizeBytes(), ignored -> new ArrayList<>()).add(item);
        }

        List<List<MediaImageItem>> candidates = new ArrayList<>();
        int totalCandidates = 0;
        for (List<MediaImageItem> sameSize : bySize.values()) {
            if (sameSize.size() > 1) {
                candidates.add(sameSize);
                totalCandidates += sameSize.size();
            }
        }

        int processed = 0;
        List<DuplicateGroup> groups = new ArrayList<>();
        int groupNumber = 1;
        for (List<MediaImageItem> candidateGroup : candidates) {
            Map<String, List<MediaImageItem>> byHash = new LinkedHashMap<>();
            for (MediaImageItem item : candidateGroup) {
                String hash = ImageHashUtils.sha256(resolver, item.getUri());
                byHash.computeIfAbsent(hash, ignored -> new ArrayList<>()).add(item);
                processed++;
                int progress = totalCandidates == 0
                        ? PROGRESS_DUPLICATE_END
                        : PROGRESS_QUERY_END + ((PROGRESS_DUPLICATE_END - PROGRESS_QUERY_END) * processed / totalCandidates);
                dispatchProgress(listener, progress, scannedCount, countMatches(groups), 0,
                        appContext.getString(R.string.scan_stage_duplicates));
            }
            for (List<MediaImageItem> hashGroup : byHash.values()) {
                if (hashGroup.size() > 1) {
                    groups.add(createDuplicateGroup(hashGroup, groupNumber++));
                    dispatchProgress(listener, progressForDuplicateStage(processed, totalCandidates),
                            scannedCount, countMatches(groups), 0,
                            appContext.getString(R.string.scan_stage_duplicates));
                }
            }
        }
        dispatchProgress(listener, PROGRESS_DUPLICATE_END, scannedCount, countMatches(groups), 0,
                appContext.getString(R.string.scan_stage_duplicates));
        return groups;
    }

    private DuplicateGroup createDuplicateGroup(List<MediaImageItem> items, int groupNumber) {
        MediaImageItem best = chooseBestItem(items);
        items.sort(groupComparator(best.getId()));
        return new DuplicateGroup("duplicate_" + groupNumber, items, best.getId());
    }

    private List<SimilarGroup> findSimilarGroups(
            List<MediaImageItem> images,
            Set<Long> duplicateMemberIds,
            int scannedCount,
            int existingMatches,
            ProgressListener listener
    ) throws IOException {
        List<MediaImageItem> candidates = new ArrayList<>();
        for (MediaImageItem item : images) {
            if (!duplicateMemberIds.contains(item.getId())) {
                candidates.add(item);
            }
        }

        List<SimilarGroup> groups = new ArrayList<>();
        Map<String, List<MediaImageItem>> buckets = new LinkedHashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            MediaImageItem item = candidates.get(i);
            item.setPerceptualHash(ImageHashUtils.dHash(appContext, item.getUri()));
            String key = buildSimilarityBucketKey(item);
            buckets.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
            int progress = PROGRESS_DUPLICATE_END + ((90 - PROGRESS_DUPLICATE_END) * (i + 1) / Math.max(1, candidates.size()));
            dispatchProgress(listener, progress, scannedCount, existingMatches, countMatches(groups),
                    appContext.getString(R.string.scan_stage_similar));
        }

        int groupNumber = 1;
        for (List<MediaImageItem> bucketItems : buckets.values()) {
            if (bucketItems.size() < 2) {
                continue;
            }
            int size = bucketItems.size();
            int[] parents = new int[size];
            for (int i = 0; i < size; i++) {
                parents[i] = i;
            }

            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++) {
                    int distance = ImageHashUtils.hammingDistance(
                            bucketItems.get(i).getPerceptualHash(),
                            bucketItems.get(j).getPerceptualHash()
                    );
                    if (distance <= SIMILARITY_THRESHOLD) {
                        union(parents, i, j);
                    }
                }
            }

            Map<Integer, List<MediaImageItem>> grouped = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                int root = find(parents, i);
                grouped.computeIfAbsent(root, ignored -> new ArrayList<>()).add(bucketItems.get(i));
            }

            for (List<MediaImageItem> groupItems : grouped.values()) {
                if (groupItems.size() > 1) {
                    MediaImageItem best = chooseBestItem(groupItems);
                    groupItems.sort(groupComparator(best.getId()));
                    groups.add(new SimilarGroup("similar_" + groupNumber, groupItems, best.getId()));
                    groupNumber++;
                    dispatchProgress(listener, 90, scannedCount, existingMatches, countMatches(groups),
                            appContext.getString(R.string.scan_stage_similar));
                }
            }
        }
        return groups;
    }

    private Comparator<MediaImageItem> groupComparator(long bestId) {
        return (first, second) -> {
            if (first.getId() == bestId) {
                return -1;
            }
            if (second.getId() == bestId) {
                return 1;
            }
            int pixelCompare = Long.compare(second.getPixelCount(), first.getPixelCount());
            if (pixelCompare != 0) {
                return pixelCompare;
            }
            return Long.compare(second.getSizeBytes(), first.getSizeBytes());
        };
    }

    private MediaImageItem chooseBestItem(List<MediaImageItem> items) {
        return Collections.max(items, Comparator
                .comparingLong(MediaImageItem::getPixelCount)
                .thenComparingLong(MediaImageItem::getDateTaken)
                .thenComparingLong(MediaImageItem::getSizeBytes));
    }

    private String buildSimilarityBucketKey(MediaImageItem item) {
        float aspectRatio = item.getHeight() == 0 ? 0f : ((float) item.getWidth() / (float) item.getHeight());
        int aspectBucket = Math.round(aspectRatio * 10f);
        int sizeBucket = (int) (Math.max(item.getWidth(), item.getHeight()) / 300f);
        return item.getBucketId() + "_" + aspectBucket + "_" + sizeBucket;
    }

    private long calculatePotentialSpace(
            List<DuplicateGroup> duplicateGroups,
            List<SimilarGroup> similarGroups
    ) {
        Set<Long> countedIds = new LinkedHashSet<>();
        long total = 0L;
        for (DuplicateGroup group : duplicateGroups) {
            total += calculateGroupPotential(group, countedIds);
        }
        for (SimilarGroup group : similarGroups) {
            total += calculateGroupPotential(group, countedIds);
        }
        return total;
    }

    private long calculateGroupPotential(MediaGroup group, Set<Long> countedIds) {
        long total = 0L;
        for (MediaImageItem item : group.getItems()) {
            if (item.getId() != group.getBestItemId() && countedIds.add(item.getId())) {
                total += item.getSizeBytes();
            }
        }
        return total;
    }

    private int countMatches(Collection<? extends MediaGroup> groups) {
        int count = 0;
        for (MediaGroup group : groups) {
            count += Math.max(0, group.getItems().size() - 1);
        }
        return count;
    }

    private int progressForDuplicateStage(int processed, int totalCandidates) {
        return totalCandidates == 0
                ? PROGRESS_DUPLICATE_END
                : PROGRESS_QUERY_END + ((PROGRESS_DUPLICATE_END - PROGRESS_QUERY_END) * processed / totalCandidates);
    }

    private void dispatchProgress(
            ProgressListener listener,
            int progress,
            int scannedCount,
            int duplicateMatchCount,
            int similarMatchCount,
            String stage
    ) {
        if (listener != null) {
            listener.onProgress(new ScanProgress(
                    progress,
                    scannedCount,
                    duplicateMatchCount,
                    similarMatchCount,
                    stage
            ));
        }
    }

    private int find(int[] parents, int index) {
        if (parents[index] != index) {
            parents[index] = find(parents, parents[index]);
        }
        return parents[index];
    }

    private void union(int[] parents, int first, int second) {
        int firstRoot = find(parents, first);
        int secondRoot = find(parents, second);
        if (firstRoot != secondRoot) {
            parents[secondRoot] = firstRoot;
        }
    }
}

