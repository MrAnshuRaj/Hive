package com.balraksh.hive.ui.video;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import com.balraksh.hive.R;
import com.balraksh.hive.adapters.VideoListAdapter;
import com.balraksh.hive.data.VideoItem;
import com.balraksh.hive.repository.VideoCompressionSessionStore;
import com.balraksh.hive.repository.VideoMediaRepository;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.BottomNavController;
import com.balraksh.hive.utils.FormatUtils;
import com.balraksh.hive.utils.PermissionHelper;
import com.balraksh.hive.video.VideoCompressionEstimateUtils;
import com.balraksh.hive.video.VideoCompressionPreset;
import com.balraksh.hive.video.VideoCompressionRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoSelectActivity extends BaseEdgeToEdgeActivity {

    public static final String EXTRA_RESET_SELECTION = "extra_reset_selection";

    private static final String STATE_SELECTED_URIS = "selected_uris";
    private static final String STATE_PRESET = "preset";
    private static final String STATE_SORT = "sort";
    private static final String STATE_ADVANCED_MODE = "advanced_mode";

    private enum SortOrder {
        SIZE_DESC,
        SIZE_ASC,
        DATE_DESC,
        DATE_ASC
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<VideoItem> pendingDeleteItems = new ArrayList<>();
    private final List<VideoItem> loadedVideos = new ArrayList<>();
    private final Set<String> selectedUris = new LinkedHashSet<>();

    private VideoMediaRepository repository;
    private VideoCompressionSessionStore sessionStore;
    private VideoListAdapter adapter;
    private VideoCompressionSettingsController settingsController;

    private MaterialButton continueButton;
    private MaterialButton qualityHighButton;
    private MaterialButton qualityMediumButton;
    private MaterialButton qualityLowButton;
    private TextView qualityHintView;
    private TextView selectAllView;
    private TextView savingsView;
    private TextView selectedSummaryView;
    private View advancedSettingsButton;
    private ImageView advancedSettingsIconView;
    private ImageView advancedChevronView;
    private View advancedSettingsOverlay;
    private View advancedSettingsSheet;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private MaterialButton emptyActionButton;

    private View advancedSettingsContentView;

    private VideoCompressionPreset selectedPreset = VideoCompressionPreset.QUICK;
    private SortOrder sortOrder = SortOrder.SIZE_DESC;
    private boolean advancedModeSelected;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (PermissionHelper.hasVideoCompressionWritePermission(this)) {
                    startCompressionForSelection();
                } else {
                    Toast.makeText(this, R.string.permission_required_toast, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, R.string.video_deleted, Toast.LENGTH_SHORT).show();
                    clearPendingDeletedSelection();
                    loadVideos();
                } else if (!pendingDeleteItems.isEmpty()) {
                    Toast.makeText(this, R.string.delete_video_permission_needed, Toast.LENGTH_SHORT).show();
                }
                pendingDeleteItems.clear();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_video_select);

        repository = new VideoMediaRepository(this);
        sessionStore = VideoCompressionSessionStore.getInstance(this);
        adapter = new VideoListAdapter(new VideoListAdapter.Listener() {
            @Override
            public void onVideoClicked(@NonNull VideoItem item) {
                toggleSelection(item);
            }

            @Override
            public void onPlayClicked(@NonNull VideoItem item) {
                playVideo(item);
            }

            @Override
            public void onVideoLongPressed(@NonNull VideoItem item, @NonNull View anchor) {
                showVideoActions(item, anchor);
            }
        });

        BottomNavController.bind(this, BottomNavController.TAB_COMPRESS);
        bindViews();
        setupAdvancedSettingsDropdown();
        bindClicks();

        if (savedInstanceState != null) {
            ArrayList<String> savedSelection = savedInstanceState.getStringArrayList(STATE_SELECTED_URIS);
            if (savedSelection != null) {
                selectedUris.addAll(savedSelection);
            }
            int savedPreset = savedInstanceState.getInt(STATE_PRESET, selectedPreset.ordinal());
            int savedSort = savedInstanceState.getInt(STATE_SORT, sortOrder.ordinal());
            selectedPreset = VideoCompressionPreset.values()[savedPreset];
            sortOrder = SortOrder.values()[savedSort];
            advancedModeSelected = savedInstanceState.getBoolean(STATE_ADVANCED_MODE, false);
        } else {
            for (VideoItem item : sessionStore.getSelectedVideos()) {
                selectedUris.add(item.getUriString());
            }
        }

        handleResetRequest(getIntent());
        settingsController.applyPreset(selectedPreset);
        bindPresetState();

        if (!PermissionHelper.hasRequiredPermissions(this)) {
            showPermissionState();
            updateSummary();
            return;
        }
        loadVideos();
        updateSummary();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleResetRequest(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PermissionHelper.hasRequiredPermissions(this)) {
            loadVideos();
        }
        updateSummary();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_SELECTED_URIS, new ArrayList<>(selectedUris));
        outState.putInt(STATE_PRESET, selectedPreset.ordinal());
        outState.putInt(STATE_SORT, sortOrder.ordinal());
        outState.putBoolean(STATE_ADVANCED_MODE, advancedModeSelected);
    }

    private void bindViews() {
        RecyclerView recyclerView = findViewById(R.id.recyclerVideos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        continueButton = findViewById(R.id.buttonContinue);
        qualityHighButton = findViewById(R.id.buttonQualityHigh);
        qualityMediumButton = findViewById(R.id.buttonQualityMedium);
        qualityLowButton = findViewById(R.id.buttonQualityLow);
        qualityHintView = findViewById(R.id.textQualityHint);
        selectAllView = findViewById(R.id.textSelectAll);
        savingsView = findViewById(R.id.textPotentialSavingsValue);
        selectedSummaryView = findViewById(R.id.textSelectedSummary);
        advancedSettingsButton = findViewById(R.id.buttonAdvancedSettings);
        advancedSettingsIconView = findViewById(R.id.imageAdvancedSettings);
        advancedChevronView = findViewById(R.id.imageAdvancedChevron);
        advancedSettingsOverlay = findViewById(R.id.layoutAdvancedSettingsOverlay);
        advancedSettingsSheet = findViewById(R.id.cardAdvancedSettingsSheet);
        advancedSettingsContentView = findViewById(R.id.cardAdvancedSettingsSheet);
        emptyState = findViewById(R.id.layoutEmptyState);
        emptyTitle = findViewById(R.id.textEmptyTitle);
        emptyMessage = findViewById(R.id.textEmptyMessage);
        emptyActionButton = findViewById(R.id.buttonEmptyAction);
    }

    private void setupAdvancedSettingsDropdown() {
        settingsController = new VideoCompressionSettingsController(
                this,
                advancedSettingsContentView.findViewById(R.id.dropdownResolution),
                advancedSettingsContentView.findViewById(R.id.dropdownFps),
                advancedSettingsContentView.findViewById(R.id.dropdownBitrate),
                advancedSettingsContentView.findViewById(R.id.dropdownAudioQuality),
                advancedSettingsContentView.findViewById(R.id.dropdownCodec)
        );
        settingsController.setupDropdowns(this::markAdvancedSelection);
        advancedSettingsContentView.findViewById(R.id.buttonAdvancedDone).setOnClickListener(v -> dismissAdvancedSettings());
        advancedSettingsContentView.findViewById(R.id.buttonAdvancedClose).setOnClickListener(v -> cancelAdvancedSettings());
    }

    private void bindClicks() {
        findViewById(R.id.buttonFilter).setOnClickListener(this::showSortMenu);
        advancedSettingsButton.setOnClickListener(v -> toggleAdvancedSettings());
        advancedSettingsOverlay.setOnClickListener(v -> cancelAdvancedSettings());
        advancedSettingsSheet.setOnClickListener(v -> { });
        continueButton.setOnClickListener(v -> ensureCompressionPermissions());
        selectAllView.setOnClickListener(v -> toggleSelectAll());
        qualityHighButton.setOnClickListener(v -> selectPreset(VideoCompressionPreset.QUICK));
        qualityMediumButton.setOnClickListener(v -> selectPreset(VideoCompressionPreset.BALANCED));
        qualityLowButton.setOnClickListener(v -> selectPreset(VideoCompressionPreset.MAX));
    }

    private void toggleAdvancedSettings() {
        if (isAdvancedSettingsVisible()) {
            cancelAdvancedSettings();
            return;
        }
        showAdvancedSettings();
    }

    private void showAdvancedSettings() {
        advancedSettingsOverlay.setVisibility(View.VISIBLE);
        advancedSettingsOverlay.setAlpha(0f);
        advancedSettingsSheet.post(() -> {
            advancedSettingsSheet.setTranslationY(advancedSettingsSheet.getHeight());
            advancedSettingsOverlay.animate()
                    .alpha(1f)
                    .setDuration(180L)
                    .start();
            advancedSettingsSheet.animate()
                    .translationY(0f)
                    .setDuration(220L)
                    .start();
        });
        updateAdvancedButtonState();
    }

    private void dismissAdvancedSettings() {
        if (!isAdvancedSettingsVisible()) {
            updateAdvancedButtonState();
            return;
        }
        advancedSettingsOverlay.animate()
                .alpha(0f)
                .setDuration(160L)
                .withEndAction(() -> {
                    advancedSettingsOverlay.setVisibility(View.GONE);
                    advancedSettingsOverlay.setAlpha(1f);
                    advancedSettingsSheet.setTranslationY(0f);
                    updateAdvancedButtonState();
                })
                .start();
        advancedSettingsSheet.animate()
                .translationY(advancedSettingsSheet.getHeight())
                .setDuration(200L)
                .start();
    }

    private void cancelAdvancedSettings() {
        selectPreset(VideoCompressionPreset.QUICK);
        dismissAdvancedSettings();
    }

    private boolean isAdvancedSettingsVisible() {
        return advancedSettingsOverlay != null && advancedSettingsOverlay.getVisibility() == View.VISIBLE;
    }

    private void updateAdvancedButtonState() {
        boolean active = advancedModeSelected || isAdvancedSettingsVisible();
        advancedSettingsButton.setBackgroundResource(active
                ? R.drawable.bg_compress_button_circle_active
                : R.drawable.bg_compress_button_circle);
        advancedSettingsIconView.setColorFilter(getColor(active ? R.color.color_scan_bg : R.color.color_scan_text_primary));
        advancedChevronView.setImageResource(isAdvancedSettingsVisible()
                ? R.drawable.ic_chevron_up
                : R.drawable.ic_chevron_down);
        advancedChevronView.setColorFilter(getColor(active ? R.color.color_scan_bg : R.color.color_scan_text_primary));
    }

    private void handleResetRequest(@Nullable Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_RESET_SELECTION, false)) {
            return;
        }
        selectedUris.clear();
        if (emptyState != null) {
            updateSummary();
            submitSortedVideos();
        }
        intent.removeExtra(EXTRA_RESET_SELECTION);
    }

    private void loadVideos() {
        emptyState.setVisibility(View.GONE);
        executor.execute(() -> {
            List<VideoItem> videos = repository.loadDeviceVideos();
            Collections.sort(videos, buildComparator());
            runOnUiThread(() -> {
                loadedVideos.clear();
                loadedVideos.addAll(videos);
                submitSortedVideos();
                if (videos.isEmpty()) {
                    showNoVideosState();
                }
            });
        });
    }

    private void submitSortedVideos() {
        List<VideoItem> displayVideos = new ArrayList<>(loadedVideos);
        Collections.sort(displayVideos, buildComparator());
        adapter.setCompressionRequest(settingsController.buildRequest(selectedPreset));
        adapter.submitList(displayVideos);
        adapter.setSelectedUris(selectedUris);
        updateSummary();
    }

    private Comparator<VideoItem> buildComparator() {
        switch (sortOrder) {
            case SIZE_ASC:
                return Comparator.comparingLong(VideoItem::getSizeBytes);
            case DATE_DESC:
                return (first, second) -> Long.compare(second.getDateAddedMs(), first.getDateAddedMs());
            case DATE_ASC:
                return Comparator.comparingLong(VideoItem::getDateAddedMs);
            case SIZE_DESC:
            default:
                return (first, second) -> Long.compare(second.getSizeBytes(), first.getSizeBytes());
        }
    }

    private void selectPreset(@NonNull VideoCompressionPreset preset) {
        selectedPreset = preset;
        advancedModeSelected = false;
        settingsController.applyPreset(preset);
        bindPresetState();
        submitSortedVideos();
    }

    private void markAdvancedSelection() {
        advancedModeSelected = true;
        bindPresetState();
        submitSortedVideos();
    }

    private void bindPresetState() {
        bindQualityButton(qualityHighButton, !advancedModeSelected && selectedPreset == VideoCompressionPreset.QUICK);
        bindQualityButton(qualityMediumButton, !advancedModeSelected && selectedPreset == VideoCompressionPreset.BALANCED);
        bindQualityButton(qualityLowButton, !advancedModeSelected && selectedPreset == VideoCompressionPreset.MAX);
        qualityHintView.setText(advancedModeSelected ? R.string.video_quality_hint_custom : getQualityHintRes(selectedPreset));
        updateAdvancedButtonState();
    }

    private void bindQualityButton(@NonNull MaterialButton button, boolean selected) {
        button.setBackgroundTintList(ColorStateList.valueOf(
                getColor(selected ? R.color.color_scan_gold : android.R.color.transparent)
        ));
        button.setTextColor(getColor(selected ? R.color.color_scan_bg : R.color.color_scan_text_secondary));
    }

    private int getQualityHintRes(@NonNull VideoCompressionPreset preset) {
        switch (preset) {
            case MAX:
                return R.string.video_quality_hint_low;
            case BALANCED:
                return R.string.video_quality_hint_medium;
            case QUICK:
            default:
                return R.string.video_quality_hint_high;
        }
    }

    private void toggleSelection(@NonNull VideoItem item) {
        if (selectedUris.contains(item.getUriString())) {
            selectedUris.remove(item.getUriString());
        } else {
            selectedUris.add(item.getUriString());
        }
        adapter.setSelectedUris(selectedUris);
        updateSummary();
    }

    private void toggleSelectAll() {
        if (loadedVideos.isEmpty()) {
            return;
        }
        if (selectedUris.size() == loadedVideos.size()) {
            selectedUris.clear();
        } else {
            selectedUris.clear();
            for (VideoItem item : loadedVideos) {
                selectedUris.add(item.getUriString());
            }
        }
        adapter.setSelectedUris(selectedUris);
        updateSummary();
    }

    private void updateSummary() {
        int count = selectedUris.size();
        long totalOriginalBytes = 0L;
        long estimatedCompressedBytes = 0L;
        VideoCompressionRequest request = settingsController.buildRequest(selectedPreset);
        for (VideoItem item : loadedVideos) {
            if (selectedUris.contains(item.getUriString())) {
                totalOriginalBytes += item.getSizeBytes();
                estimatedCompressedBytes += VideoCompressionEstimateUtils.estimateCompressedBytes(item, request);
            }
        }
        long estimatedSavingsBytes = Math.max(0L, totalOriginalBytes - estimatedCompressedBytes);

        continueButton.setEnabled(count > 0);
        savingsView.setText(FormatUtils.formatStorage(estimatedSavingsBytes));
        selectedSummaryView.setText(count == 1
                ? "1 video selected"
                : count + " videos selected");
        selectAllView.setText(count == loadedVideos.size() && count > 0
                ? getString(R.string.deselect_all)
                : getString(R.string.select_all));
    }

    private void ensureCompressionPermissions() {
        if (selectedUris.isEmpty()) {
            Toast.makeText(this, R.string.select_at_least_one_video, Toast.LENGTH_SHORT).show();
            return;
        }
        if (PermissionHelper.hasVideoCompressionWritePermission(this)) {
            startCompressionForSelection();
            return;
        }
        permissionLauncher.launch(PermissionHelper.getVideoCompressionPermissions());
    }

    private void startCompressionForSelection() {
        List<VideoItem> selectedVideos = new ArrayList<>();
        for (VideoItem item : loadedVideos) {
            if (selectedUris.contains(item.getUriString())) {
                selectedVideos.add(item);
            }
        }
        if (selectedVideos.isEmpty()) {
            Toast.makeText(this, R.string.select_at_least_one_video, Toast.LENGTH_SHORT).show();
            return;
        }
        sessionStore.setSelectedVideos(selectedVideos);
        sessionStore.startCompression(settingsController.buildRequest(selectedPreset));
        startActivity(new Intent(this, VideoCompressingActivity.class));
    }

    private void showSortMenu(@NonNull View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(Menu.NONE, 1, 1, getString(R.string.video_sort_largest));
        popupMenu.getMenu().add(Menu.NONE, 2, 2, getString(R.string.video_sort_smallest));
        popupMenu.getMenu().add(Menu.NONE, 3, 3, getString(R.string.video_sort_newest));
        popupMenu.getMenu().add(Menu.NONE, 4, 4, getString(R.string.video_sort_oldest));
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                sortOrder = SortOrder.SIZE_DESC;
            } else if (itemId == 2) {
                sortOrder = SortOrder.SIZE_ASC;
            } else if (itemId == 3) {
                sortOrder = SortOrder.DATE_DESC;
            } else if (itemId == 4) {
                sortOrder = SortOrder.DATE_ASC;
            }
            submitSortedVideos();
            return true;
        });
        popupMenu.show();
    }

    private void showVideoActions(@NonNull VideoItem item, @NonNull View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(0, 1, 1, getString(R.string.play));
        popupMenu.getMenu().add(0, 2, 2, getString(R.string.open));
        popupMenu.getMenu().add(0, 3, 3, getString(R.string.share));
        popupMenu.getMenu().add(0, 4, 4, getString(R.string.delete_video));
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == 1) {
                playVideo(item);
                return true;
            }
            if (itemId == 2) {
                openVideo(item);
                return true;
            }
            if (itemId == 3) {
                shareVideo(item);
                return true;
            }
            if (itemId == 4) {
                confirmDelete(item);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void playVideo(@NonNull VideoItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(item.getUri(), "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.play)));
    }

    private void openVideo(@NonNull VideoItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(item.getUri(), "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void shareVideo(@NonNull VideoItem item) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, item.getUri());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void confirmDelete(@NonNull VideoItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_video_title)
                .setMessage(R.string.delete_video_message)
                .setPositiveButton(R.string.delete_video, (dialog, which) -> deleteVideo(item))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteVideo(@NonNull VideoItem item) {
        pendingDeleteItems.clear();
        pendingDeleteItems.add(item);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                PendingIntent pendingIntent = android.provider.MediaStore.createDeleteRequest(
                        getContentResolver(),
                        Collections.singletonList(item.getUri())
                );
                deleteRequestLauncher.launch(new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build());
            } catch (Exception exception) {
                pendingDeleteItems.clear();
                Toast.makeText(this, R.string.delete_video_failed, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        executor.execute(() -> {
            boolean deleted;
            try {
                deleted = getContentResolver().delete(item.getUri(), null, null) > 0;
            } catch (SecurityException exception) {
                deleted = false;
            }
            boolean wasDeleted = deleted;
            runOnUiThread(() -> {
                pendingDeleteItems.clear();
                if (wasDeleted) {
                    Toast.makeText(this, R.string.video_deleted, Toast.LENGTH_SHORT).show();
                    removeDeletedItem(item);
                } else {
                    Toast.makeText(this, R.string.delete_video_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void clearPendingDeletedSelection() {
        for (VideoItem item : pendingDeleteItems) {
            selectedUris.remove(item.getUriString());
        }
        updateSummary();
    }

    private void removeDeletedItem(@NonNull VideoItem item) {
        selectedUris.remove(item.getUriString());
        loadedVideos.remove(item);
        submitSortedVideos();
        if (loadedVideos.isEmpty()) {
            showNoVideosState();
        }
    }

    private void showNoVideosState() {
        emptyActionButton.setVisibility(View.GONE);
        emptyTitle.setText(R.string.no_videos_found);
        emptyMessage.setText(R.string.no_videos_found_message);
        emptyState.setVisibility(View.VISIBLE);
    }

    private void showPermissionState() {
        emptyTitle.setText(R.string.videos_access);
        emptyMessage.setText(R.string.video_permission_message);
        emptyActionButton.setVisibility(View.VISIBLE);
        emptyActionButton.setText(R.string.open_settings_for_video);
        emptyActionButton.setOnClickListener(v -> PermissionHelper.openAppSettings(this));
        emptyState.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
