package com.balraksh.hive.video;

public enum VideoAudioQualityOption {
    HIGH(192_000),
    MEDIUM(128_000),
    LOW(96_000);

    private final int bitrate;

    VideoAudioQualityOption(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getBitrate() {
        return bitrate;
    }
}

