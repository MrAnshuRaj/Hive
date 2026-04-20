package com.balraksh.hive.repository;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.balraksh.hive.data.BucketOption;
import com.balraksh.hive.data.SwipeMediaItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SwipeMediaRepository {

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private final ContentResolver resolver;

    public SwipeMediaRepository(@NonNull Context context) {
        resolver = context.getApplicationContext().getContentResolver();
    }

    @NonNull
    public List<SwipeMediaItem> loadMedia() {
        List<SwipeMediaItem> items = new ArrayList<>();
        items.addAll(queryImages());
        items.addAll(queryVideos());
        markBestShots(items);
        items.sort((first, second) -> Long.compare(second.getDateMillis(), first.getDateMillis()));
        return items;
    }

    @NonNull
    public List<BucketOption> buildBuckets(@NonNull List<SwipeMediaItem> items) {
        Map<Long, BucketOption> buckets = new LinkedHashMap<>();
        for (SwipeMediaItem item : items) {
            BucketOption current = buckets.get(item.getBucketId());
            if (current == null) {
                buckets.put(item.getBucketId(), new BucketOption(
                        item.getBucketId(),
                        item.getBucketName(),
                        1,
                        item.getSizeBytes()
                ));
            } else {
                buckets.put(item.getBucketId(), new BucketOption(
                        item.getBucketId(),
                        current.getBucketName(),
                        current.getItemCount() + 1,
                        current.getTotalBytes() + item.getSizeBytes()
                ));
            }
        }
        List<BucketOption> result = new ArrayList<>(buckets.values());
        result.sort((first, second) -> Integer.compare(second.getItemCount(), first.getItemCount()));
        return result;
    }

    @NonNull
    private List<SwipeMediaItem> queryImages() {
        List<SwipeMediaItem> items = new ArrayList<>();
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
                return items;
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

            while (cursor.moveToNext()) {
                long size = Math.max(0L, cursor.getLong(sizeIndex));
                if (size <= 0L) {
                    continue;
                }
                long id = cursor.getLong(idIndex);
                String bucketName = safeLabel(cursor.getString(bucketNameIndex), "Camera Roll");
                String displayName = safeLabel(cursor.getString(nameIndex), "photo_" + id + ".jpg");
                long dateTaken = cursor.getLong(dateTakenIndex);
                long dateAdded = cursor.getLong(dateAddedIndex) * 1000L;
                items.add(new SwipeMediaItem(
                        id,
                        SwipeMediaItem.TYPE_IMAGE,
                        ContentUris.withAppendedId(collection, id),
                        displayName,
                        size,
                        dateTaken > 0L ? dateTaken : dateAdded,
                        cursor.getInt(widthIndex),
                        cursor.getInt(heightIndex),
                        0L,
                        cursor.getLong(bucketIdIndex),
                        bucketName,
                        safeLabel(cursor.getString(pathIndex), bucketName)
                ));
            }
        }
        return items;
    }

    @NonNull
    private List<SwipeMediaItem> queryVideos() {
        List<SwipeMediaItem> items = new ArrayList<>();
        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String pathColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? MediaStore.Video.Media.RELATIVE_PATH
                : MediaStore.Video.Media.DATA;
        String[] projection = new String[]{
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                pathColumn
        };

        try (Cursor cursor = resolver.query(
                collection,
                projection,
                null,
                null,
                MediaStore.Video.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor == null) {
                return items;
            }
            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            int durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
            int widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);
            int heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);
            int bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID);
            int bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
            int pathIndex = cursor.getColumnIndexOrThrow(pathColumn);

            while (cursor.moveToNext()) {
                long size = Math.max(0L, cursor.getLong(sizeIndex));
                long duration = Math.max(0L, cursor.getLong(durationIndex));
                if (size <= 0L || duration <= 0L) {
                    continue;
                }
                long id = cursor.getLong(idIndex);
                String bucketName = safeLabel(cursor.getString(bucketNameIndex), "Videos");
                items.add(new SwipeMediaItem(
                        id,
                        SwipeMediaItem.TYPE_VIDEO,
                        ContentUris.withAppendedId(collection, id),
                        safeLabel(cursor.getString(nameIndex), "video_" + id + ".mp4"),
                        size,
                        cursor.getLong(dateAddedIndex) * 1000L,
                        cursor.getInt(widthIndex),
                        cursor.getInt(heightIndex),
                        duration,
                        cursor.getLong(bucketIdIndex),
                        bucketName,
                        safeLabel(cursor.getString(pathIndex), bucketName)
                ));
            }
        }
        return items;
    }

    private void markBestShots(@NonNull List<SwipeMediaItem> items) {
        Map<String, SwipeMediaItem> bestByCluster = new LinkedHashMap<>();
        for (SwipeMediaItem item : items) {
            if (item.isVideo()) {
                continue;
            }
            String key = item.getBucketId() + "_" + (item.getDateMillis() / DAY_MS);
            SwipeMediaItem current = bestByCluster.get(key);
            if (current == null || compareBest(item, current) > 0) {
                bestByCluster.put(key, item);
            }
        }
        for (SwipeMediaItem item : bestByCluster.values()) {
            item.setBestShot(true);
        }
    }

    private int compareBest(@NonNull SwipeMediaItem first, @NonNull SwipeMediaItem second) {
        return Comparator
                .comparingLong(SwipeMediaItem::getPixelCount)
                .thenComparingLong(SwipeMediaItem::getSizeBytes)
                .thenComparingLong(SwipeMediaItem::getDateMillis)
                .compare(first, second);
    }

    @NonNull
    private String safeLabel(String value, @NonNull String fallback) {
        if (TextUtils.isEmpty(value)) {
            return fallback;
        }
        return value;
    }
}
