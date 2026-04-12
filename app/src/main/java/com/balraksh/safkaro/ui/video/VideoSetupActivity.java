package com.balraksh.safkaro.ui.video;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.data.VideoItem;
import com.balraksh.safkaro.repository.VideoCompressionSessionStore;
import com.balraksh.safkaro.ui.BaseEdgeToEdgeActivity;
import com.balraksh.safkaro.utils.FormatUtils;
import com.balraksh.safkaro.utils.PermissionHelper;
import com.balraksh.safkaro.video.CodecSupportUtils;
import com.balraksh.safkaro.video.VideoAudioQualityOption;
import com.balraksh.safkaro.video.VideoBitrateOption;
import com.balraksh.safkaro.video.VideoCodecOption;
import com.balraksh.safkaro.video.VideoCompressionPreset;
import com.balraksh.safkaro.video.VideoCompressionRequest;
import com.balraksh.safkaro.video.VideoFpsOption;
import com.balraksh.safkaro.video.VideoResolutionOption;

import java.util.ArrayList;
import java.util.List;

public class VideoSetupActivity extends BaseEdgeToEdgeActivity {

    private VideoCompressionSessionStore sessionStore;
    private List<VideoItem> selectedVideos;

    private MaterialCardView quickCard;
    private MaterialCardView balancedCard;
    private MaterialCardView maxCard;
    private MaterialCardView advancedCard;
    private MaterialAutoCompleteTextView resolutionDropdown;
    private MaterialAutoCompleteTextView fpsDropdown;
    private MaterialAutoCompleteTextView bitrateDropdown;
    private MaterialAutoCompleteTextView audioQualityDropdown;
    private MaterialAutoCompleteTextView codecDropdown;
    private ImageView advancedChevron;
    private android.view.View advancedContent;
    private List<VideoCodecOption> codecOptions = new ArrayList<>();

    private VideoCompressionPreset selectedPreset = VideoCompressionPreset.BALANCED;
    private boolean advancedExpanded;
    private boolean advancedModeSelected;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (PermissionHelper.hasVideoCompressionWritePermission(this)) {
                    startCompression();
                } else {
                    Toast.makeText(this, R.string.permission_required_toast, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_video_setup);

        sessionStore = VideoCompressionSessionStore.getInstance(this);
        selectedVideos = sessionStore.getSelectedVideos();
        if (selectedVideos.isEmpty()) {
            finish();
            return;
        }

        quickCard = findViewById(R.id.cardQuick);
        balancedCard = findViewById(R.id.cardBalanced);
        maxCard = findViewById(R.id.cardMax);
        advancedCard = findViewById(R.id.cardAdvancedOptions);
        resolutionDropdown = findViewById(R.id.dropdownResolution);
        fpsDropdown = findViewById(R.id.dropdownFps);
        bitrateDropdown = findViewById(R.id.dropdownBitrate);
        audioQualityDropdown = findViewById(R.id.dropdownAudioQuality);
        codecDropdown = findViewById(R.id.dropdownCodec);
        advancedChevron = findViewById(R.id.imageAdvancedChevron);
        advancedContent = findViewById(R.id.layoutAdvancedContent);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        advancedCard.setOnClickListener(v -> toggleAdvanced());
        findViewById(R.id.layoutAdvancedHeader).setOnClickListener(v -> toggleAdvanced());
        findViewById(R.id.buttonCompressNow).setOnClickListener(v -> ensureCompressionPermissions());

        setupDropdowns();
        bindSelectedSummary();

        quickCard.setOnClickListener(v -> selectPreset(VideoCompressionPreset.QUICK));
        balancedCard.setOnClickListener(v -> selectPreset(VideoCompressionPreset.BALANCED));
        maxCard.setOnClickListener(v -> selectPreset(VideoCompressionPreset.MAX));

        selectPreset(selectedPreset);
        bindAdvancedState();
    }

    private void bindSelectedSummary() {
        int count = selectedVideos.size();
        long totalBytes = 0L;
        for (VideoItem item : selectedVideos) {
            totalBytes += item.getSizeBytes();
        }
        TextView summaryView = findViewById(R.id.textSelectedSummary);
        summaryView.setText(getString(
                count == 1 ? R.string.selected_videos_summary : R.string.selected_videos_summary_plural,
                count,
                FormatUtils.formatStorage(totalBytes)
        ));
    }

    private void setupDropdowns() {
        resolutionDropdown.setSimpleItems(new String[]{
                getString(R.string.resolution_original),
                getString(R.string.resolution_1080),
                getString(R.string.resolution_720),
                getString(R.string.resolution_480)
        });
        bitrateDropdown.setSimpleItems(new String[]{
                getString(R.string.compression_strength_low),
                getString(R.string.compression_strength_medium),
                getString(R.string.compression_strength_high)
        });
        fpsDropdown.setSimpleItems(new String[]{
                getString(R.string.fps_original),
                getString(R.string.fps_30),
                getString(R.string.fps_24)
        });
        audioQualityDropdown.setSimpleItems(new String[]{
                getString(R.string.compression_strength_high),
                getString(R.string.compression_strength_medium),
                getString(R.string.compression_strength_low)
        });
        codecOptions = CodecSupportUtils.getSupportedVideoCodecOptions();
        List<String> codecLabels = new ArrayList<>();
        for (VideoCodecOption option : codecOptions) {
            codecLabels.add(option.getDisplayName());
        }
        codecDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, codecLabels));
        resolutionDropdown.setOnItemClickListener((parent, view, position, id) -> markAdvancedSelection());
        bitrateDropdown.setOnItemClickListener((parent, view, position, id) -> markAdvancedSelection());
        fpsDropdown.setOnItemClickListener((parent, view, position, id) -> markAdvancedSelection());
        audioQualityDropdown.setOnItemClickListener((parent, view, position, id) -> markAdvancedSelection());
        codecDropdown.setOnItemClickListener((parent, view, position, id) -> markAdvancedSelection());
    }

    private void selectPreset(@NonNull VideoCompressionPreset preset) {
        selectedPreset = preset;
        advancedModeSelected = false;
        switch (preset) {
            case QUICK:
                resolutionDropdown.setText(getString(R.string.resolution_original), false);
                bitrateDropdown.setText(getString(R.string.compression_strength_low), false);
                fpsDropdown.setText(getString(R.string.fps_original), false);
                audioQualityDropdown.setText(getString(R.string.compression_strength_high), false);
                codecDropdown.setText(getCodecDisplayName(CodecSupportUtils.MIME_AVC), false);
                break;
            case BALANCED:
                resolutionDropdown.setText(getString(R.string.resolution_1080), false);
                bitrateDropdown.setText(getString(R.string.compression_strength_medium), false);
                fpsDropdown.setText(getString(R.string.fps_30), false);
                audioQualityDropdown.setText(getString(R.string.compression_strength_medium), false);
                codecDropdown.setText(getCodecDisplayName(resolveBalancedCodecMimeType()), false);
                break;
            case MAX:
                resolutionDropdown.setText(getString(R.string.resolution_720), false);
                bitrateDropdown.setText(getString(R.string.compression_strength_high), false);
                fpsDropdown.setText(getString(R.string.fps_24), false);
                audioQualityDropdown.setText(getString(R.string.compression_strength_low), false);
                codecDropdown.setText(getCodecDisplayName(resolveMaxCodecMimeType()), false);
                break;
        }
        bindPresetState();
        bindAdvancedState();
    }

    private void bindPresetState() {
        bindCardSelection(quickCard, !advancedModeSelected && selectedPreset == VideoCompressionPreset.QUICK);
        bindCardSelection(balancedCard, !advancedModeSelected && selectedPreset == VideoCompressionPreset.BALANCED);
        bindCardSelection(maxCard, !advancedModeSelected && selectedPreset == VideoCompressionPreset.MAX);
    }

    private void bindCardSelection(MaterialCardView cardView, boolean selected) {
        cardView.setStrokeColor(ContextCompat.getColor(this, selected ? R.color.color_primary : R.color.color_border));
        cardView.setStrokeWidth((int) (getResources().getDisplayMetrics().density * (selected ? 2 : 1)));
    }

    private void toggleAdvanced() {
        advancedExpanded = !advancedExpanded;
        if (advancedExpanded) {
            advancedModeSelected = true;
        }
        bindPresetState();
        bindAdvancedState();
    }

    private void bindAdvancedState() {
        advancedContent.setVisibility(advancedExpanded ? android.view.View.VISIBLE : android.view.View.GONE);
        advancedChevron.setImageResource(advancedExpanded ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);
        bindCardSelection(advancedCard, advancedExpanded || advancedModeSelected);
    }

    private void ensureCompressionPermissions() {
        if (PermissionHelper.hasVideoCompressionWritePermission(this)) {
            startCompression();
            return;
        }
        permissionLauncher.launch(PermissionHelper.getVideoCompressionPermissions());
    }

    private void startCompression() {
        sessionStore.startCompression(new VideoCompressionRequest(
                selectedPreset,
                resolveResolutionOption(),
                resolveFpsOption(),
                resolveBitrateOption(),
                resolveCodecMimeType(),
                resolveAudioQualityOption().getBitrate()
        ));
        startActivity(new android.content.Intent(this, VideoCompressingActivity.class));
    }

    @NonNull
    private VideoResolutionOption resolveResolutionOption() {
        CharSequence text = resolutionDropdown.getText();
        if (getString(R.string.resolution_1080).contentEquals(text)) {
            return VideoResolutionOption.P1080;
        }
        if (getString(R.string.resolution_720).contentEquals(text)) {
            return VideoResolutionOption.P720;
        }
        if (getString(R.string.resolution_480).contentEquals(text)) {
            return VideoResolutionOption.P480;
        }
        return VideoResolutionOption.ORIGINAL;
    }

    @NonNull
    private VideoFpsOption resolveFpsOption() {
        CharSequence text = fpsDropdown.getText();
        if (getString(R.string.fps_30).contentEquals(text)) {
            return VideoFpsOption.FPS_30;
        }
        if (getString(R.string.fps_24).contentEquals(text)) {
            return VideoFpsOption.FPS_24;
        }
        return VideoFpsOption.ORIGINAL;
    }

    @NonNull
    private VideoBitrateOption resolveBitrateOption() {
        CharSequence text = bitrateDropdown.getText();
        if (getString(R.string.compression_strength_high).contentEquals(text)) {
            return VideoBitrateOption.HIGH;
        }
        if (getString(R.string.compression_strength_medium).contentEquals(text)) {
            return VideoBitrateOption.MEDIUM;
        }
        return VideoBitrateOption.LOW;
    }

    @NonNull
    private VideoAudioQualityOption resolveAudioQualityOption() {
        CharSequence text = audioQualityDropdown.getText();
        if (getString(R.string.compression_strength_low).contentEquals(text)) {
            return VideoAudioQualityOption.LOW;
        }
        if (getString(R.string.compression_strength_medium).contentEquals(text)) {
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

    private void markAdvancedSelection() {
        advancedModeSelected = true;
        bindPresetState();
        bindAdvancedState();
    }
}
