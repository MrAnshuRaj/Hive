package com.balraksh.hive.video;

public enum VideoFpsOption {
    ORIGINAL(0),
    FPS_30(30),
    FPS_24(24);

    private final int value;

    VideoFpsOption(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

