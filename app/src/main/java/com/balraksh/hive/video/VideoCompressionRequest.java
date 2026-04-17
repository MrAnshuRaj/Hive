package com.balraksh.hive.video;

import androidx.annotation.NonNull;

public class VideoCompressionRequest {

    private final VideoCompressionPreset preset;
    private final VideoResolutionOption resolutionOption;
    private final VideoFpsOption fpsOption;
    private final VideoBitrateOption bitrateOption;
    private final String selectedCodecMimeType;
    private final int audioBitrate;

    public VideoCompressionRequest(
            @NonNull VideoCompressionPreset preset,
            @NonNull VideoResolutionOption resolutionOption,
            @NonNull VideoFpsOption fpsOption,
            @NonNull VideoBitrateOption bitrateOption,
            @NonNull String selectedCodecMimeType,
            int audioBitrate
    ) {
        this.preset = preset;
        this.resolutionOption = resolutionOption;
        this.fpsOption = fpsOption;
        this.bitrateOption = bitrateOption;
        this.selectedCodecMimeType = selectedCodecMimeType;
        this.audioBitrate = audioBitrate;
    }

    @NonNull
    public VideoCompressionPreset getPreset() {
        return preset;
    }

    @NonNull
    public VideoResolutionOption getResolutionOption() {
        return resolutionOption;
    }

    @NonNull
    public VideoFpsOption getFpsOption() {
        return fpsOption;
    }

    @NonNull
    public VideoBitrateOption getBitrateOption() {
        return bitrateOption;
    }

    @NonNull
    public String getSelectedCodecMimeType() {
        return selectedCodecMimeType;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }
}

