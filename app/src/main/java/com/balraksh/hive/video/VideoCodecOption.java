package com.balraksh.hive.video;

import androidx.annotation.NonNull;

import java.util.Objects;

public class VideoCodecOption {

    private final String mimeType;
    private final String displayName;

    public VideoCodecOption(@NonNull String mimeType, @NonNull String displayName) {
        this.mimeType = mimeType;
        this.displayName = displayName;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof VideoCodecOption)) {
            return false;
        }
        VideoCodecOption that = (VideoCodecOption) object;
        return mimeType.equals(that.mimeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mimeType);
    }
}

