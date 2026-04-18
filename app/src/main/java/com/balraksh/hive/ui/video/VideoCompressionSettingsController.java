package com.balraksh.hive.ui.video;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import com.balraksh.hive.R;
import com.balraksh.hive.video.CodecSupportUtils;
import com.balraksh.hive.video.VideoAudioQualityOption;
import com.balraksh.hive.video.VideoBitrateOption;
import com.balraksh.hive.video.VideoCodecOption;
import com.balraksh.hive.video.VideoCompressionPreset;
import com.balraksh.hive.video.VideoCompressionRequest;
import com.balraksh.hive.video.VideoFpsOption;
import com.balraksh.hive.video.VideoResolutionOption;

import java.util.ArrayList;
import java.util.List;

final class VideoCompressionSettingsController {

    private final Context context;
    private final MaterialAutoCompleteTextView resolutionDropdown;
    private final MaterialAutoCompleteTextView fpsDropdown;
    private final MaterialAutoCompleteTextView bitrateDropdown;
    private final MaterialAutoCompleteTextView audioQualityDropdown;
    private final MaterialAutoCompleteTextView codecDropdown;

    private List<VideoCodecOption> codecOptions = new ArrayList<>();

    VideoCompressionSettingsController(
            @NonNull Context context,
            @NonNull MaterialAutoCompleteTextView resolutionDropdown,
            @NonNull MaterialAutoCompleteTextView fpsDropdown,
            @NonNull MaterialAutoCompleteTextView bitrateDropdown,
            @NonNull MaterialAutoCompleteTextView audioQualityDropdown,
            @NonNull MaterialAutoCompleteTextView codecDropdown
    ) {
        this.context = context;
        this.resolutionDropdown = resolutionDropdown;
        this.fpsDropdown = fpsDropdown;
        this.bitrateDropdown = bitrateDropdown;
        this.audioQualityDropdown = audioQualityDropdown;
        this.codecDropdown = codecDropdown;
    }

    void setupDropdowns(@NonNull Runnable onAdvancedSelection) {
        resolutionDropdown.setSimpleItems(new String[]{
                context.getString(R.string.resolution_original),
                context.getString(R.string.resolution_1080),
                context.getString(R.string.resolution_720),
                context.getString(R.string.resolution_480)
        });
        bitrateDropdown.setSimpleItems(new String[]{
                context.getString(R.string.compression_strength_low),
                context.getString(R.string.compression_strength_medium),
                context.getString(R.string.compression_strength_high)
        });
        fpsDropdown.setSimpleItems(new String[]{
                context.getString(R.string.fps_original),
                context.getString(R.string.fps_30),
                context.getString(R.string.fps_24)
        });
        audioQualityDropdown.setSimpleItems(new String[]{
                context.getString(R.string.compression_strength_high),
                context.getString(R.string.compression_strength_medium),
                context.getString(R.string.compression_strength_low)
        });
        codecOptions = CodecSupportUtils.getSupportedVideoCodecOptions();
        List<String> codecLabels = new ArrayList<>();
        for (VideoCodecOption option : codecOptions) {
            codecLabels.add(option.getDisplayName());
        }
        codecDropdown.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, codecLabels));

        resolutionDropdown.setOnItemClickListener((parent, view, position, id) -> {
            keepTextStartVisible(resolutionDropdown);
            onAdvancedSelection.run();
        });
        bitrateDropdown.setOnItemClickListener((parent, view, position, id) -> {
            keepTextStartVisible(bitrateDropdown);
            onAdvancedSelection.run();
        });
        fpsDropdown.setOnItemClickListener((parent, view, position, id) -> {
            keepTextStartVisible(fpsDropdown);
            onAdvancedSelection.run();
        });
        audioQualityDropdown.setOnItemClickListener((parent, view, position, id) -> {
            keepTextStartVisible(audioQualityDropdown);
            onAdvancedSelection.run();
        });
        codecDropdown.setOnItemClickListener((parent, view, position, id) -> {
            keepTextStartVisible(codecDropdown);
            onAdvancedSelection.run();
        });
    }

    void applyPreset(@NonNull VideoCompressionPreset preset) {
        switch (preset) {
            case QUICK:
                setDropdownText(resolutionDropdown, context.getString(R.string.resolution_original));
                setDropdownText(bitrateDropdown, context.getString(R.string.compression_strength_low));
                setDropdownText(fpsDropdown, context.getString(R.string.fps_original));
                setDropdownText(audioQualityDropdown, context.getString(R.string.compression_strength_high));
                setDropdownText(codecDropdown, getCodecDisplayName(CodecSupportUtils.MIME_AVC));
                break;
            case BALANCED:
                setDropdownText(resolutionDropdown, context.getString(R.string.resolution_1080));
                setDropdownText(bitrateDropdown, context.getString(R.string.compression_strength_medium));
                setDropdownText(fpsDropdown, context.getString(R.string.fps_30));
                setDropdownText(audioQualityDropdown, context.getString(R.string.compression_strength_medium));
                setDropdownText(codecDropdown, getCodecDisplayName(resolveBalancedCodecMimeType()));
                break;
            case MAX:
                setDropdownText(resolutionDropdown, context.getString(R.string.resolution_720));
                setDropdownText(bitrateDropdown, context.getString(R.string.compression_strength_high));
                setDropdownText(fpsDropdown, context.getString(R.string.fps_24));
                setDropdownText(audioQualityDropdown, context.getString(R.string.compression_strength_low));
                setDropdownText(codecDropdown, getCodecDisplayName(resolveMaxCodecMimeType()));
                break;
        }
    }

    @NonNull
    VideoCompressionRequest buildRequest(@NonNull VideoCompressionPreset preset) {
        return new VideoCompressionRequest(
                preset,
                resolveResolutionOption(),
                resolveFpsOption(),
                resolveBitrateOption(),
                resolveCodecMimeType(),
                resolveAudioQualityOption().getBitrate()
        );
    }

    @NonNull
    private VideoResolutionOption resolveResolutionOption() {
        CharSequence text = resolutionDropdown.getText();
        if (context.getString(R.string.resolution_1080).contentEquals(text)) {
            return VideoResolutionOption.P1080;
        }
        if (context.getString(R.string.resolution_720).contentEquals(text)) {
            return VideoResolutionOption.P720;
        }
        if (context.getString(R.string.resolution_480).contentEquals(text)) {
            return VideoResolutionOption.P480;
        }
        return VideoResolutionOption.ORIGINAL;
    }

    @NonNull
    private VideoFpsOption resolveFpsOption() {
        CharSequence text = fpsDropdown.getText();
        if (context.getString(R.string.fps_30).contentEquals(text)) {
            return VideoFpsOption.FPS_30;
        }
        if (context.getString(R.string.fps_24).contentEquals(text)) {
            return VideoFpsOption.FPS_24;
        }
        return VideoFpsOption.ORIGINAL;
    }

    @NonNull
    private VideoBitrateOption resolveBitrateOption() {
        CharSequence text = bitrateDropdown.getText();
        if (context.getString(R.string.compression_strength_high).contentEquals(text)) {
            return VideoBitrateOption.HIGH;
        }
        if (context.getString(R.string.compression_strength_medium).contentEquals(text)) {
            return VideoBitrateOption.MEDIUM;
        }
        return VideoBitrateOption.LOW;
    }

    @NonNull
    private VideoAudioQualityOption resolveAudioQualityOption() {
        CharSequence text = audioQualityDropdown.getText();
        if (context.getString(R.string.compression_strength_low).contentEquals(text)) {
            return VideoAudioQualityOption.LOW;
        }
        if (context.getString(R.string.compression_strength_medium).contentEquals(text)) {
            return VideoAudioQualityOption.MEDIUM;
        }
        return VideoAudioQualityOption.HIGH;
    }

    @NonNull
    private String resolveCodecMimeType() {
        CharSequence text = codecDropdown.getText();
        for (VideoCodecOption option : codecOptions) {
            if (option.getDisplayName().contentEquals(text)) {
                return option.getMimeType();
            }
        }
        return CodecSupportUtils.getDefaultCodecMimeType(codecOptions);
    }

    @NonNull
    private String resolveBalancedCodecMimeType() {
        if (CodecSupportUtils.hasCodec(codecOptions, CodecSupportUtils.MIME_HEVC)) {
            return CodecSupportUtils.MIME_HEVC;
        }
        return CodecSupportUtils.getDefaultCodecMimeType(codecOptions);
    }

    @NonNull
    private String resolveMaxCodecMimeType() {
        return resolveBalancedCodecMimeType();
    }

    @NonNull
    private String getCodecDisplayName(@NonNull String mimeType) {
        for (VideoCodecOption option : codecOptions) {
            if (mimeType.equals(option.getMimeType())) {
                return option.getDisplayName();
            }
        }
        return CodecSupportUtils.getDisplayName(CodecSupportUtils.getDefaultCodecMimeType(codecOptions));
    }

    private void setDropdownText(@NonNull MaterialAutoCompleteTextView dropdown, @NonNull String value) {
        dropdown.setText(value, false);
        keepTextStartVisible(dropdown);
    }

    private void keepTextStartVisible(@NonNull MaterialAutoCompleteTextView dropdown) {
        dropdown.post(() -> {
            dropdown.setSelection(0);
            dropdown.scrollTo(0, 0);
        });
    }
}
