package com.balraksh.hive.video;

public enum VideoResolutionOption {
    ORIGINAL(0),
    P1080(1080),
    P720(720),
    P480(480);

    private final int shortSide;

    VideoResolutionOption(int shortSide) {
        this.shortSide = shortSide;
    }

    public int getShortSide() {
        return shortSide;
    }
}

