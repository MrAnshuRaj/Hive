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
import com.balraksh.safkaro.video.VideoBitrateOption;
import com.balraksh.safkaro.video.VideoCompressionPreset;
import com.balraksh.safkaro.video.VideoCompressionRequest;
import com.balraksh.safkaro.video.VideoFpsOption;
import com.balraksh.safkaro.video.VideoResolutionOption;

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
    private ImageView advancedChevron;
    private android.view.View advancedContent;

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
        fpsDropdown.setSimpleItems(new String[]{
                getString(R.string.fps_original),
                getString(R.string.fps_30),
                getString(R.string.fps_24)
        });
        bitrateDropdown.setSimpleItems(new String[]{
                getString(R.string.compression_strength_low),
                getString(R.string.compression_strength_medium),
                getString(R.string.compression_strength_high)
        });
    }

    private void selectPreset(@NonNull VideoCompressionPreset preset) {
        selectedPreset = preset;
        advancedModeSelected = false;
        switch (preset) {
            case QUICK:
                resolutionDropdown.setText(getString(R.string.resolution_original), false);
                fpsDropdown.setText(getString(R.string.fps_original), false);
                bitrateDropdown.setText(getString(R.string.compression_strength_low), false);
                break;
            case BALANCED:
                resolutionDropdown.setText(getString(R.string.resolution_1080), false);
                fpsDropdown.setText(getString(R.string.fps_30), false);
                bitrateDropdown.setText(getString(R.string.compression_strength_medium), false);
                break;
            case MAX:
                resolutionDropdown.setText(getString(R.string.resolution_720), false);
                fpsDropdown.setText(getString(R.string.fps_24), false);
                bitrateDropdown.setText(getString(R.string.compression_strength_high), false);
                break;
        }
        bindPresetState();
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
        advancedModeSelected = advancedExpanded;
        bindPresetState();
        bindAdvancedState();
    }

    private void bindAdvancedState() {
        advancedContent.setVisibility(advancedExpanded ? android.view.View.VISIBLE : android.view.View.GONE);
        advancedChevron.setImageResource(advancedExpanded ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);
        bindCardSelection(advancedCard, advancedExpanded);
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
                resolveBitrateOption()
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
}
