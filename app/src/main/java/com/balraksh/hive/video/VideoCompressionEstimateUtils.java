package com.balraksh.hive.video;

import androidx.annotation.NonNull;

import com.balraksh.hive.data.VideoItem;

public final class VideoCompressionEstimateUtils {

    private static final int DEFAULT_AUDIO_BITRATE = 128_000;
    private static final float MIN_SCALE = 0.32f;
    private static final float MAX_SCALE = 0.92f;

    private VideoCompressionEstimateUtils() {
    }

    public static long estimateCompressedBytes(
            @NonNull VideoItem item,
            @NonNull VideoCompressionRequest request
    ) {
        long sizeBytes = Math.max(1L, item.getSizeBytes());
        double durationSeconds = Math.max(1d, item.getDurationMs() / 1000d);
        double sourceTotalBitrate = Math.max(256_000d, (sizeBytes * 8d) / durationSeconds);

        double sourceAudioBitrate = Math.min(192_000d, Math.max(64_000d, sourceTotalBitrate * 0.08d));
        double sourceVideoBitrate = Math.max(96_000d, sourceTotalBitrate - sourceAudioBitrate);
        double targetAudioBitrate = Math.min(sourceAudioBitrate, Math.max(48_000, request.getAudioBitrate()));

        double codecFactor = CodecSupportUtils.MIME_HEVC.equals(request.getSelectedCodecMimeType()) ? 0.92d : 1.0d;
        double fpsFactor = resolveFpsFactor(request);
        double resolutionFactor = resolveResolutionFactor(item, request);

        double targetVideoBitrate = sourceVideoBitrate
                * request.getBitrateOption().getSourceMultiplier()
                * codecFactor
                * fpsFactor
                * resolutionFactor;
        double estimatedScale = clamp((targetVideoBitrate + targetAudioBitrate) / sourceTotalBitrate, MIN_SCALE, MAX_SCALE);

        return Math.max(1L, Math.round(sizeBytes * estimatedScale));
    }

    public static float estimateCompressionScale(
            @NonNull VideoItem item,
            @NonNull VideoCompressionRequest request
    ) {
        return (float) estimateCompressedBytes(item, request) / (float) Math.max(1L, item.getSizeBytes());
    }

    private static double resolveFpsFactor(@NonNull VideoCompressionRequest request) {
        int targetFps = request.getFpsOption().getValue();
        if (targetFps <= 0) {
            return 1.0d;
        }
        double fpsRatio = Math.min(1d, targetFps / 30d);
        return 0.85d + (fpsRatio * 0.15d);
    }

    private static double resolveResolutionFactor(
            @NonNull VideoItem item,
            @NonNull VideoCompressionRequest request
    ) {
        int targetShortSide = request.getResolutionOption().getShortSide();
        int sourceShortSide = Math.min(item.getWidth(), item.getHeight());
        if (targetShortSide <= 0 || sourceShortSide <= 0 || sourceShortSide <= targetShortSide) {
            return 1.0d;
        }
        double scale = targetShortSide / (double) sourceShortSide;
        double areaRatio = scale * scale;
        return 0.90d + (areaRatio * 0.10d);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
