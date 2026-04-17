package com.balraksh.hive.video;

public final class VideoCompressionSettingsResolver {

    private static final int DEFAULT_FPS = 30;
    private static final int DEFAULT_BITRATE = 3_000_000;
    private static final int BITRATE_FLOOR = 450_000;

    private VideoCompressionSettingsResolver() {
    }

    public static ResolvedVideoCompressionSettings resolve(
            VideoMetadata metadata,
            VideoCompressionRequest request
    ) {
        int sourceWidth = ensureEven(Math.max(2, metadata.getDisplayWidth()));
        int sourceHeight = ensureEven(Math.max(2, metadata.getDisplayHeight()));
        int sourceShortSide = Math.min(sourceWidth, sourceHeight);

        int targetShortSide = request.getResolutionOption().getShortSide();
        int outputWidth = sourceWidth;
        int outputHeight = sourceHeight;
        if (targetShortSide > 0 && sourceShortSide > targetShortSide) {
            float scale = (float) targetShortSide / (float) sourceShortSide;
            outputWidth = ensureEven(Math.round(sourceWidth * scale));
            outputHeight = ensureEven(Math.round(sourceHeight * scale));
        } else {
            targetShortSide = 0;
        }

        int sourceFps = metadata.getFrameRate() > 0f
                ? Math.round(metadata.getFrameRate())
                : DEFAULT_FPS;
        int requestedFps = request.getFpsOption().getValue();
        int targetFps = requestedFps > 0 ? Math.min(sourceFps, requestedFps) : sourceFps;
        targetFps = Math.max(12, targetFps);

        int sourceBitrate = metadata.getBitrate() > 0
                ? metadata.getBitrate()
                : estimateBitrate(metadata.getSizeBytes(), metadata.getDurationMs());
        int floor = Math.min(BITRATE_FLOOR, Math.max(120_000, (int) (sourceBitrate * 0.9f)));
        int targetBitrate = (int) (sourceBitrate * request.getBitrateOption().getSourceMultiplier());
        targetBitrate = Math.max(floor, targetBitrate);
        targetBitrate = Math.min(targetBitrate, Math.max(96_000, (int) (sourceBitrate * 0.95f)));

        String targetVideoMimeType = CodecSupportUtils.isCodecUsable(request.getSelectedCodecMimeType())
                ? request.getSelectedCodecMimeType()
                : CodecSupportUtils.MIME_AVC;

        int targetAudioBitrate = 0;
        if (metadata.hasAudioTrack()) {
            targetAudioBitrate = request.getAudioBitrate();
            if (metadata.getAudioBitrate() > 0) {
                targetAudioBitrate = Math.min(metadata.getAudioBitrate(), targetAudioBitrate);
            }
        }

        return new ResolvedVideoCompressionSettings(
                outputWidth,
                outputHeight,
                Math.max(96_000, targetBitrate),
                targetFps,
                targetShortSide,
                targetVideoMimeType,
                targetAudioBitrate,
                metadata.hasAudioTrack()
        );
    }

    private static int estimateBitrate(long sizeBytes, long durationMs) {
        if (sizeBytes <= 0L || durationMs <= 0L) {
            return DEFAULT_BITRATE;
        }
        long durationSeconds = Math.max(1L, durationMs / 1000L);
        long bitsPerSecond = (sizeBytes * 8L) / durationSeconds;
        return (int) Math.max(256_000L, Math.min(Integer.MAX_VALUE, bitsPerSecond));
    }

    private static int ensureEven(int value) {
        return value % 2 == 0 ? value : value - 1;
    }
}

