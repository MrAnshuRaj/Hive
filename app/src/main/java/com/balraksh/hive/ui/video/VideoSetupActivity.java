package com.balraksh.hive.ui.video;

import android.os.Bundle;
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

import com.balraksh.hive.R;
import com.balraksh.hive.data.VideoItem;
import com.balraksh.hive.repository.VideoCompressionSessionStore;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.utils.FormatUtils;
import com.balraksh.hive.utils.PermissionHelper;
import com.balraksh.hive.video.VideoCompressionPreset;

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
    private VideoCompressionSettingsController settingsController;

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
        settingsController = new VideoCompressionSettingsController(
                this,
                resolutionDropdown,
                fpsDropdown,
                bitrateDropdown,
                audioQualityDropdown,
                codecDropdown
        );

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        advancedCard.setOnClickListener(v -> toggleAdvanced());
        findViewById(R.id.layoutAdvancedHeader).setOnClickListener(v -> toggleAdvanced());
        findViewById(R.id.buttonCompressNow).setOnClickListener(v -> ensureCompressionPermissions());

        settingsController.setupDropdowns(this::markAdvancedSelection);
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

    private void selectPreset(@NonNull VideoCompressionPreset preset) {
        selectedPreset = preset;
        advancedModeSelected = false;
        settingsController.applyPreset(preset);
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
        sessionStore.startCompression(settingsController.buildRequest(selectedPreset));
        startActivity(new android.content.Intent(this, VideoCompressingActivity.class));
    }

    private void markAdvancedSelection() {
        advancedModeSelected = true;
        bindPresetState();
        bindAdvancedState();
    }
}

