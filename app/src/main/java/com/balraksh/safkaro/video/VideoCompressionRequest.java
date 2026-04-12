package com.balraksh.safkaro.video;

import androidx.annotation.NonNull;

public class VideoCompressionRequest {

    private final VideoCompressionPreset preset;
    private final VideoResolutionOption resolutionOption;
    private final VideoFpsOption fpsOption;
    private final VideoBitrateOption bitrateOption;

    public VideoCompressionRequest(
            @NonNull VideoCompressionPreset preset,
            @NonNull VideoResolutionOption resolutionOption,
            @NonNull VideoFpsOption fpsOption,
            @NonNull VideoBitrateOption bitrateOption
    ) {
        this.preset = preset;
        this.resolutionOption = resolutionOption;
        this.fpsOption = fpsOption;
        this.bitrateOption = bitrateOption;
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
}
