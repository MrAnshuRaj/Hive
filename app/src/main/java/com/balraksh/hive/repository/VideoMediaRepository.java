package com.balraksh.hive.repository;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.balraksh.hive.data.VideoItem;
import com.balraksh.hive.video.VideoMetadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VideoMediaRepository {

    private static final String OUTPUT_RELATIVE_PATH = Environment.DIRECTORY_MOVIES + "/Hive";

    private final Context appContext;
    private final ContentResolver resolver;

    public VideoMediaRepository(Context context) {
        appContext = context.getApplicationContext();
        resolver = appContext.getContentResolver();
    }

    @NonNull
    public List<VideoItem> loadDeviceVideos() {
        List<VideoItem> videos = new ArrayList<>();
        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
        };

        try (Cursor cursor = resolver.query(
                collection,
                projection,
                null,
                null,
                MediaStore.Video.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor == null) {
                return videos;
            }

            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            int durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);
            int heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                String name = cursor.getString(nameIndex);
                long size = cursor.getLong(sizeIndex);
                long duration = cursor.getLong(durationIndex);
                int width = cursor.getInt(widthIndex);
                int height = cursor.getInt(heightIndex);
                if (size <= 0L || duration <= 0L) {
                    continue;
                }
                if (TextUtils.isEmpty(name)) {
                    name = String.format(Locale.US, "video_%d.mp4", id);
                }
                videos.add(new VideoItem(
                        id,
                        ContentUris.withAppendedId(collection, id),
                        name,
                        size,
                        duration,
                        width,
                        height
                ));
            }
        }
        return videos;
    }

    @NonNull
    public VideoMetadata loadMetadata(@NonNull VideoItem item) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(appContext, item.getUri());
            long durationMs = parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION), item.getDurationMs());
            int width = parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH), item.getWidth());
            int height = parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT), item.getHeight());
            int rotation = parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION), 0);
            int bitrate = parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE), 0);

            float frameRate = 0f;
            boolean hasAudioTrack = false;
            int audioBitrate = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                frameRate = parseFloat(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE), 0f);
            }

            MediaExtractor extractor = new MediaExtractor();
            try {
                extractor.setDataSource(appContext, item.getUri(), null);
                for (int trackIndex = 0; trackIndex < extractor.getTrackCount(); trackIndex++) {
                    MediaFormat format = extractor.getTrackFormat(trackIndex);
                    String mimeType = format.getString(MediaFormat.KEY_MIME);
                    if (mimeType != null && mimeType.startsWith("video/")) {
                        if (bitrate <= 0 && format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                        }
                        if (frameRate <= 0f && format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                        }
                    } else if (mimeType != null && mimeType.startsWith("audio/")) {
                        hasAudioTrack = true;
                        if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            audioBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                        }
                    }
                }
            } finally {
                extractor.release();
            }

            return new VideoMetadata(
                    item.getUri(),
                    item.getDisplayName(),
                    item.getSizeBytes(),
                    durationMs,
                    width,
                    height,
                    rotation,
                    bitrate,
                    frameRate,
                    hasAudioTrack,
                    audioBitrate
            );
        } finally {
            retriever.release();
        }
    }

    @Nullable
    public Uri saveCompressedVideo(@NonNull File sourceFile, @NonNull String displayName) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, displayName);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");

        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.RELATIVE_PATH, OUTPUT_RELATIVE_PATH);
            values.put(MediaStore.Video.Media.IS_PENDING, 1);
        } else {
            File outputDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Hive");
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw new IOException("Unable to create output directory");
            }
            values.put(MediaStore.Video.Media.DATA, new File(outputDir, displayName).getAbsolutePath());
        }

        Uri destinationUri = resolver.insert(collection, values);
        if (destinationUri == null) {
            throw new IOException("Unable to create destination media entry");
        }

        try (InputStream inputStream = new FileInputStream(sourceFile);
             OutputStream outputStream = resolver.openOutputStream(destinationUri, "w")) {
            if (outputStream == null) {
                throw new IOException("Unable to open destination output stream");
            }
            copy(inputStream, outputStream);
        } catch (IOException ioException) {
            resolver.delete(destinationUri, null, null);
            throw ioException;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues publishValues = new ContentValues();
            publishValues.put(MediaStore.Video.Media.IS_PENDING, 0);
            resolver.update(destinationUri, publishValues, null, null);
        }
        return destinationUri;
    }

    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
    }

    private long parseLong(@Nullable String value, long fallback) {
        if (TextUtils.isEmpty(value)) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private int parseInt(@Nullable String value, int fallback) {
        if (TextUtils.isEmpty(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private float parseFloat(@Nullable String value, float fallback) {
        if (TextUtils.isEmpty(value)) {
            return fallback;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}

