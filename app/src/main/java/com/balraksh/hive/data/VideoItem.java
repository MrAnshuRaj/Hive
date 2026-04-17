package com.balraksh.hive.data;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Objects;

public class VideoItem {

    private final long id;
    private final String uriString;
    private final String displayName;
    private final long sizeBytes;
    private final long durationMs;
    private final int width;
    private final int height;

    public VideoItem(
            long id,
            @NonNull Uri uri,
            @NonNull String displayName,
            long sizeBytes,
            long durationMs,
            int width,
            int height
    ) {
        this.id = id;
        this.uriString = uri.toString();
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
        this.durationMs = durationMs;
        this.width = width;
        this.height = height;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public Uri getUri() {
        return Uri.parse(uriString);
    }

    @NonNull
    public String getUriString() {
        return uriString;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VideoItem)) {
            return false;
        }
        VideoItem that = (VideoItem) other;
        return Objects.equals(uriString, that.uriString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uriString);
    }
}

