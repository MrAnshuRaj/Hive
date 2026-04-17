package com.balraksh.hive.video;

public class ResolvedVideoCompressionSettings {

    private final int outputWidth;
    private final int outputHeight;
    private final int targetBitrate;
    private final int targetFrameRate;
    private final int targetShortSide;
    private final String targetVideoMimeType;
    private final int targetAudioBitrate;
    private final boolean hasAudioTrack;

    public ResolvedVideoCompressionSettings(
            int outputWidth,
            int outputHeight,
            int targetBitrate,
            int targetFrameRate,
            int targetShortSide,
            String targetVideoMimeType,
            int targetAudioBitrate,
            boolean hasAudioTrack
    ) {
        this.outputWidth = outputWidth;
        this.outputHeight = outputHeight;
        this.targetBitrate = targetBitrate;
        this.targetFrameRate = targetFrameRate;
        this.targetShortSide = targetShortSide;
        this.targetVideoMimeType = targetVideoMimeType;
        this.targetAudioBitrate = targetAudioBitrate;
        this.hasAudioTrack = hasAudioTrack;
    }

    public int getOutputWidth() {
        return outputWidth;
    }

    public int getOutputHeight() {
        return outputHeight;
    }

    public int getTargetBitrate() {
        return targetBitrate;
    }

    public int getTargetFrameRate() {
        return targetFrameRate;
    }

    public int getTargetShortSide() {
        return targetShortSide;
    }

    public String getTargetVideoMimeType() {
        return targetVideoMimeType;
    }

    public int getTargetAudioBitrate() {
        return targetAudioBitrate;
    }

    public boolean hasAudioTrack() {
        return hasAudioTrack;
    }
}

