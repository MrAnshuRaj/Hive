package com.balraksh.safkaro.video;

public class ResolvedVideoCompressionSettings {

    private final int outputWidth;
    private final int outputHeight;
    private final int targetBitrate;
    private final int targetFrameRate;
    private final int targetShortSide;

    public ResolvedVideoCompressionSettings(
            int outputWidth,
            int outputHeight,
            int targetBitrate,
            int targetFrameRate,
            int targetShortSide
    ) {
        this.outputWidth = outputWidth;
        this.outputHeight = outputHeight;
        this.targetBitrate = targetBitrate;
        this.targetFrameRate = targetFrameRate;
        this.targetShortSide = targetShortSide;
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
}
