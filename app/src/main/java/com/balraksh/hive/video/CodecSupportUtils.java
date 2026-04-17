package com.balraksh.hive.video;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CodecSupportUtils {

    public static final String MIME_AVC = "video/avc";
    public static final String MIME_HEVC = "video/hevc";
    public static final String MIME_AV1 = "video/av01";
    public static final String MIME_VP9 = "video/x-vnd.on2.vp9";

    private CodecSupportUtils() {
    }

    @NonNull
    public static List<VideoCodecOption> getSupportedVideoCodecOptions() {
        Map<String, VideoCodecOption> options = new LinkedHashMap<>();
        for (MediaCodecInfo codecInfo : new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos()) {
            if (!codecInfo.isEncoder()) {
                continue;
            }
            for (String supportedType : codecInfo.getSupportedTypes()) {
                String normalizedMimeType = supportedType == null ? "" : supportedType.toLowerCase(Locale.US);
                if (!isCodecUsable(normalizedMimeType)) {
                    continue;
                }
                MediaCodecInfo.CodecCapabilities capabilities;
                try {
                    capabilities = codecInfo.getCapabilitiesForType(supportedType);
                } catch (IllegalArgumentException exception) {
                    continue;
                }
                if (!supportsSurfaceInput(capabilities)) {
                    continue;
                }
                options.put(normalizedMimeType, new VideoCodecOption(normalizedMimeType, getDisplayName(normalizedMimeType)));
            }
        }
        return new ArrayList<>(options.values());
    }

    public static boolean isCodecUsable(@NonNull String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return false;
        }
        switch (mimeType) {
            case MIME_AVC:
            case MIME_HEVC:
                return true;
            default:
                return false;
        }
    }

    @NonNull
    public static String getDefaultCodecMimeType(@NonNull List<VideoCodecOption> options) {
        for (VideoCodecOption option : options) {
            if (MIME_AVC.equals(option.getMimeType())) {
                return MIME_AVC;
            }
        }
        return options.isEmpty() ? MIME_AVC : options.get(0).getMimeType();
    }

    public static boolean hasCodec(@NonNull List<VideoCodecOption> options, @NonNull String mimeType) {
        for (VideoCodecOption option : options) {
            if (mimeType.equals(option.getMimeType())) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public static String getDisplayName(@NonNull String mimeType) {
        switch (mimeType) {
            case MIME_HEVC:
                return "H.265 (HEVC)";
            case MIME_AV1:
                return "AV1";
            case MIME_VP9:
                return "VP9";
            case MIME_AVC:
            default:
                return "H.264 (AVC)";
        }
    }

    private static boolean supportsSurfaceInput(@NonNull MediaCodecInfo.CodecCapabilities capabilities) {
        for (int colorFormat : capabilities.colorFormats) {
            if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface) {
                return true;
            }
        }
        return false;
    }
}

