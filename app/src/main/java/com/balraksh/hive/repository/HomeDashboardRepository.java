package com.balraksh.hive.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.balraksh.hive.R;
import com.balraksh.hive.data.CleanupHistoryEntry;
import com.balraksh.hive.data.HomeDashboardData;
import com.balraksh.hive.data.MediaGroup;
import com.balraksh.hive.data.QuickCleanupItem;
import com.balraksh.hive.data.ScanResult;
import com.balraksh.hive.data.SmartInsightItem;
import com.balraksh.hive.data.StorageCategoryUsage;
import com.balraksh.hive.data.VideoCompressionHistoryEntry;
import com.balraksh.hive.data.VideoItem;
import com.balraksh.hive.data.WeeklySummary;
import com.balraksh.hive.utils.FormatUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeDashboardRepository {

    private static final long LARGE_VIDEO_THRESHOLD_BYTES = 150L * 1024L * 1024L;
    private static final long WEEK_WINDOW_MS = 7L * 24L * 60L * 60L * 1000L;

    private final Context appContext;
    private final ContentResolver resolver;
    private final CleanupPreferences cleanupPreferences;
    private final VideoCompressionPreferences videoCompressionPreferences;
    private final VideoMediaRepository videoMediaRepository;

    public HomeDashboardRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        resolver = appContext.getContentResolver();
        cleanupPreferences = new CleanupPreferences(appContext);
        videoCompressionPreferences = new VideoCompressionPreferences(appContext);
        videoMediaRepository = new VideoMediaRepository(appContext);
    }

    @NonNull
    public HomeDashboardData load() {
        StorageNumbers storageNumbers = loadStorageNumbers();
        WeeklySummary weeklySummary = loadWeeklySummary();
        QuickCleanupSnapshot quickCleanup = loadQuickCleanupSnapshot();
        List<StorageCategoryUsage> categories = buildCategories(storageNumbers);
        List<QuickCleanupItem> quickCleanupItems = buildQuickCleanupItems(quickCleanup);
        List<SmartInsightItem> smartInsights = buildSmartInsights(quickCleanup);
        return new HomeDashboardData(
                storageNumbers.totalBytes,
                storageNumbers.usedBytes,
                Math.max(storageNumbers.totalBytes / 10L, 4L * 1024L * 1024L * 1024L),
                categories,
                weeklySummary,
                quickCleanupItems,
                smartInsights
        );
    }

    private StorageNumbers loadStorageNumbers() {
        File storageRoot = Environment.getDataDirectory();
        StatFs statFs = new StatFs(storageRoot.getAbsolutePath());
        long totalBytes = statFs.getTotalBytes();
        long availableBytes = statFs.getAvailableBytes();
        long usedBytes = Math.max(0L, totalBytes - availableBytes);

        long photosBytes = queryMediaSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        long videosBytes = queryMediaSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        long musicBytes = queryMediaSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        long documentsBytes = queryDocumentLikeBytes();

        long knownBytes = photosBytes + videosBytes + musicBytes + documentsBytes;
        long remaining = Math.max(0L, usedBytes - knownBytes);
        long appsEstimate = (long) (remaining * 0.38f);
        long otherBytes = Math.max(0L, remaining - appsEstimate);

        return new StorageNumbers(
                totalBytes,
                usedBytes,
                photosBytes,
                videosBytes,
                documentsBytes,
                musicBytes,
                appsEstimate,
                otherBytes
        );
    }

    private long queryMediaSize(@NonNull android.net.Uri collectionUri) {
        long totalBytes = 0L;
        try (Cursor cursor = resolver.query(
                collectionUri,
                new String[]{MediaStore.MediaColumns.SIZE},
                null,
                null,
                null
        )) {
            if (cursor == null) {
                return 0L;
            }
            int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
            while (cursor.moveToNext()) {
                totalBytes += Math.max(0L, cursor.getLong(sizeIndex));
            }
        }
        return totalBytes;
    }

    private long queryDocumentLikeBytes() {
        long totalBytes = 0L;
        try (Cursor cursor = resolver.query(
                MediaStore.Files.getContentUri("external"),
                new String[]{MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.MEDIA_TYPE},
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=?",
                new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE)},
                null
        )) {
            if (cursor == null) {
                return 0L;
            }
            int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            while (cursor.moveToNext()) {
                totalBytes += Math.max(0L, cursor.getLong(sizeIndex));
            }
        } catch (Exception ignored) {
        }
        return totalBytes;
    }

    private WeeklySummary loadWeeklySummary() {
        long cutoff = System.currentTimeMillis() - WEEK_WINDOW_MS;
        long spaceFreedBytes = 0L;
        int filesOrganized = 0;
        for (CleanupHistoryEntry entry : cleanupPreferences.getHistory()) {
            if (entry.getTimestampMillis() < cutoff) {
                continue;
            }
            spaceFreedBytes += entry.getFreedBytes();
            filesOrganized += entry.getDeletedCount();
        }

        int videosCompressed = 0;
        for (VideoCompressionHistoryEntry entry : videoCompressionPreferences.getHistory()) {
            if (entry.getTimestampMillis() < cutoff) {
                continue;
            }
            spaceFreedBytes += entry.getSavedBytes();
            videosCompressed += entry.getCompressedCount();
        }
        return new WeeklySummary(spaceFreedBytes, filesOrganized, videosCompressed);
    }

    private QuickCleanupSnapshot loadQuickCleanupSnapshot() {
        ScanResult currentResult = ScanSessionStore.getInstance().getCurrentResult();
        int duplicateCount = cleanupPreferences.getLastDuplicateCount();
        long duplicateBytes = cleanupPreferences.getLastDuplicateBytes();
        int similarCount = cleanupPreferences.getLastSimilarCount();
        long similarBytes = cleanupPreferences.getLastSimilarBytes();
        if (currentResult != null) {
            duplicateCount = currentResult.getDuplicateMatchCount();
            similarCount = currentResult.getSimilarMatchCount();
            duplicateBytes = calculatePotentialBytes(currentResult.getDuplicateGroups());
            similarBytes = calculatePotentialBytes(currentResult.getSimilarGroups());
        }

        int screenshotCount = 0;
        long screenshotBytes = 0L;
        String pathColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? MediaStore.Images.Media.RELATIVE_PATH
                : MediaStore.Images.Media.DATA;
        try (Cursor cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                        pathColumn,
                        MediaStore.Images.Media.SIZE
                },
                null,
                null,
                null
        )) {
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                int pathIndex = cursor.getColumnIndexOrThrow(pathColumn);
                int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
                while (cursor.moveToNext()) {
                    String joined = (safe(cursor.getString(nameIndex)) + " "
                            + safe(cursor.getString(bucketIndex)) + " "
                            + safe(cursor.getString(pathIndex))).toLowerCase();
                    if (joined.contains("screenshot") || joined.contains("screen_shot") || joined.contains("screen-shot")) {
                        screenshotCount++;
                        screenshotBytes += Math.max(0L, cursor.getLong(sizeIndex));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        List<VideoItem> deviceVideos = videoMediaRepository.loadDeviceVideos();
        int largeVideoCount = 0;
        long largeVideoBytes = 0L;
        for (VideoItem videoItem : deviceVideos) {
            if (videoItem.getSizeBytes() >= LARGE_VIDEO_THRESHOLD_BYTES) {
                largeVideoCount++;
                largeVideoBytes += videoItem.getSizeBytes();
            }
        }

        int compressibleVideoCount = Math.max(largeVideoCount, Math.min(12, deviceVideos.size()));
        long compressibleVideoBytes = Math.max(largeVideoBytes, estimateTopVideoBytes(deviceVideos));
        return new QuickCleanupSnapshot(
                duplicateCount,
                duplicateBytes,
                similarCount,
                similarBytes,
                screenshotCount,
                screenshotBytes,
                largeVideoCount,
                largeVideoBytes,
                compressibleVideoCount,
                compressibleVideoBytes
        );
    }

    private long estimateTopVideoBytes(List<VideoItem> deviceVideos) {
        long total = 0L;
        int count = 0;
        for (VideoItem videoItem : deviceVideos) {
            total += videoItem.getSizeBytes();
            count++;
            if (count >= 12) {
                break;
            }
        }
        return total;
    }

    private List<StorageCategoryUsage> buildCategories(StorageNumbers storageNumbers) {
        List<StorageCategoryUsage> categories = new ArrayList<>();
        categories.add(new StorageCategoryUsage("Videos", storageNumbers.videosBytes, getColor(R.color.color_onboarding_gold)));
        categories.add(new StorageCategoryUsage("Photos", storageNumbers.photosBytes, getColor(R.color.color_onboarding_orange)));
        categories.add(new StorageCategoryUsage("Docs", storageNumbers.documentsBytes, 0xFFE39B63));
        categories.add(new StorageCategoryUsage("Music", storageNumbers.musicBytes, 0xFFF5C324));
        categories.add(new StorageCategoryUsage("Apps", storageNumbers.appsBytes, 0xFFE7C154));
        categories.add(new StorageCategoryUsage("Other", storageNumbers.otherBytes, 0xFFE7D28D));
        return categories;
    }

    private List<QuickCleanupItem> buildQuickCleanupItems(QuickCleanupSnapshot quickCleanup) {
        List<QuickCleanupItem> items = new ArrayList<>();
        items.add(new QuickCleanupItem(
                QuickCleanupItem.TYPE_DUPLICATES,
                appContext.getString(R.string.quick_cleanup_duplicates),
                quickCleanup.duplicateCount,
                quickCleanup.duplicateBytes
        ));
        items.add(new QuickCleanupItem(
                QuickCleanupItem.TYPE_SIMILAR,
                appContext.getString(R.string.quick_cleanup_similar),
                quickCleanup.similarCount,
                quickCleanup.similarBytes
        ));
        items.add(new QuickCleanupItem(
                QuickCleanupItem.TYPE_LARGE_VIDEOS,
                appContext.getString(R.string.quick_cleanup_large_videos),
                quickCleanup.largeVideoCount,
                quickCleanup.largeVideoBytes
        ));
        return items;
    }

    private List<SmartInsightItem> buildSmartInsights(QuickCleanupSnapshot quickCleanup) {
        List<SmartInsightItem> items = new ArrayList<>();
        items.add(new SmartInsightItem(
                SmartInsightItem.ACTION_COMPRESS_VIDEOS,
                R.drawable.ic_video,
                appContext.getString(R.string.smart_insight_compress_title, quickCleanup.compressibleVideoCount),
                appContext.getString(
                        R.string.smart_insight_compress_subtitle,
                        FormatUtils.formatStorage(Math.max(quickCleanup.compressibleVideoBytes, LARGE_VIDEO_THRESHOLD_BYTES))
                ),
                appContext.getString(R.string.smart_insight_compress_cta)
        ));
        items.add(new SmartInsightItem(
                SmartInsightItem.ACTION_REMOVE_DUPLICATES,
                R.drawable.ic_photo,
                appContext.getString(R.string.smart_insight_duplicates_title, quickCleanup.duplicateCount),
                appContext.getString(
                        R.string.smart_insight_duplicates_subtitle,
                        FormatUtils.formatStorage(Math.max(quickCleanup.duplicateBytes, cleanupPreferences.getLastScanPotentialBytes()))
                ),
                appContext.getString(R.string.smart_insight_duplicates_cta)
        ));
        return items;
    }

    private long calculatePotentialBytes(List<? extends MediaGroup> groups) {
        long total = 0L;
        for (MediaGroup group : groups) {
            for (com.balraksh.hive.data.MediaImageItem item : group.getItems()) {
                if (item.getId() != group.getBestItemId()) {
                    total += item.getSizeBytes();
                }
            }
        }
        return total;
    }

    @ColorInt
    private int getColor(int colorRes) {
        return ContextCompat.getColor(appContext, colorRes);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class StorageNumbers {
        final long totalBytes;
        final long usedBytes;
        final long photosBytes;
        final long videosBytes;
        final long documentsBytes;
        final long musicBytes;
        final long appsBytes;
        final long otherBytes;

        StorageNumbers(
                long totalBytes,
                long usedBytes,
                long photosBytes,
                long videosBytes,
                long documentsBytes,
                long musicBytes,
                long appsBytes,
                long otherBytes
        ) {
            this.totalBytes = totalBytes;
            this.usedBytes = usedBytes;
            this.photosBytes = photosBytes;
            this.videosBytes = videosBytes;
            this.documentsBytes = documentsBytes;
            this.musicBytes = musicBytes;
            this.appsBytes = appsBytes;
            this.otherBytes = otherBytes;
        }
    }

    private static final class QuickCleanupSnapshot {
        final int duplicateCount;
        final long duplicateBytes;
        final int similarCount;
        final long similarBytes;
        final int screenshotCount;
        final long screenshotBytes;
        final int largeVideoCount;
        final long largeVideoBytes;
        final int compressibleVideoCount;
        final long compressibleVideoBytes;

        QuickCleanupSnapshot(
                int duplicateCount,
                long duplicateBytes,
                int similarCount,
                long similarBytes,
                int screenshotCount,
                long screenshotBytes,
                int largeVideoCount,
                long largeVideoBytes,
                int compressibleVideoCount,
                long compressibleVideoBytes
        ) {
            this.duplicateCount = duplicateCount;
            this.duplicateBytes = duplicateBytes;
            this.similarCount = similarCount;
            this.similarBytes = similarBytes;
            this.screenshotCount = screenshotCount;
            this.screenshotBytes = screenshotBytes;
            this.largeVideoCount = largeVideoCount;
            this.largeVideoBytes = largeVideoBytes;
            this.compressibleVideoCount = compressibleVideoCount;
            this.compressibleVideoBytes = compressibleVideoBytes;
        }
    }
}
