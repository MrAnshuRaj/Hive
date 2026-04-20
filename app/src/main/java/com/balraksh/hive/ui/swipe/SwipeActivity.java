package com.balraksh.hive.ui.swipe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import com.balraksh.hive.R;
import com.balraksh.hive.adapters.FolderOptionAdapter;
import com.balraksh.hive.adapters.SwipeTrashAdapter;
import com.balraksh.hive.data.BucketOption;
import com.balraksh.hive.data.CleanupHistoryEntry;
import com.balraksh.hive.data.SwipeMediaItem;
import com.balraksh.hive.repository.CleanupPreferences;
import com.balraksh.hive.repository.SwipeMediaRepository;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.BottomNavController;
import com.balraksh.hive.utils.FormatUtils;
import com.balraksh.hive.utils.PermissionHelper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SwipeActivity extends BaseEdgeToEdgeActivity {

    private enum ScreenMode {
        SWIPE,
        TRASH,
        FOLDERS
    }

    private enum MediaFilter {
        ALL,
        IMAGES,
        VIDEOS
    }

    private enum SortOrder {
        SIZE_DESC,
        SIZE_ASC,
        TIME_DESC,
        TIME_ASC
    }

    private static final long VIDEO_PROGRESS_DURATION_MS = 4000L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final List<SwipeMediaItem> allMedia = new ArrayList<>();
    private final List<SwipeMediaItem> filteredMedia = new ArrayList<>();
    private final List<BucketOption> bucketOptions = new ArrayList<>();
    private final List<SwipeDecision> history = new ArrayList<>();
    private final Set<Long> selectedBucketIds = new LinkedHashSet<>();
    private final Set<String> trashSelectionKeys = new LinkedHashSet<>();
    private final List<SwipeMediaItem> pendingDeleteItems = new ArrayList<>();

    private SwipeMediaRepository repository;
    private CleanupPreferences cleanupPreferences;
    private FolderOptionAdapter folderAdapter;
    private SwipeTrashAdapter trashAdapter;

    private View buttonBack;
    private View buttonUndo;
    private View buttonFilter;
    private View viewFilterDot;
    private TextView titleText;
    private TextView subtitleText;

    private View swipeStateView;
    private View emptyStateView;
    private View doneStateView;
    private View trashStateView;
    private View foldersStateView;
    private View permissionStateView;

    private TextView emptyTitleText;
    private TextView emptyMessageText;
    private MaterialButton emptyActionButton;
    private TextView permissionTitleText;
    private TextView permissionMessageText;
    private MaterialButton permissionActionButton;

    private SwipeDragLayout frontCardContainer;
    private FrameLayout backCardContainer;
    private View keepLabel;
    private View deleteLabel;

    private View swipeActionsView;
    private MaterialButton deleteActionButton;
    private MaterialButton keepActionButton;
    private TextView deleteBadgeText;
    private TextView keepBadgeText;
    private View deleteBadgeView;
    private View keepBadgeView;
    private MaterialButton continueActionButton;
    private MaterialButton doneActionButton;

    private RecyclerView trashRecyclerView;
    private TextView trashSummaryCountText;
    private TextView trashSummarySizeText;
    private MaterialButton deleteNowButton;

    private RecyclerView foldersRecyclerView;
    private MaterialButton applyFoldersButton;

    private View filterOverlay;
    private MaterialCardView filterSheet;
    private MaterialButton filterAllButton;
    private MaterialButton filterImagesButton;
    private MaterialButton filterVideosButton;
    private MaterialButton sortLargestButton;
    private MaterialButton sortSmallestButton;
    private MaterialButton sortNewestButton;
    private MaterialButton sortOldestButton;
    private TextView foldersSummaryText;

    private View metadataOverlay;
    private MaterialCardView metadataSheet;
    private TextView metadataTitleText;
    private TextView metadataTypeText;
    private TextView metadataSizeText;
    private TextView metadataResolutionText;
    private TextView metadataAlbumText;
    private TextView metadataDateText;
    private TextView metadataDurationText;

    private SwipeCardHolder frontCardHolder;
    private SwipeCardHolder backCardHolder;

    private ScreenMode screenMode = ScreenMode.SWIPE;
    private MediaFilter activeFilter = MediaFilter.ALL;
    private SortOrder activeSort = SortOrder.SIZE_DESC;
    private int currentIndex = 0;
    private boolean cardAnimating;
    private boolean deleteInProgress;
    @Nullable
    private String playingKey;
    @Nullable
    private ValueAnimator videoProgressAnimator;
    @Nullable
    private SwipeMediaItem selectedMetadataItem;

    private final ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        verifyDeleteRequestOutcome(new ArrayList<>(pendingDeleteItems));
                    } else {
                        deleteLegacyItems(new ArrayList<>(pendingDeleteItems));
                    }
                } else {
                    deleteInProgress = false;
                    pendingDeleteItems.clear();
                    bindAll();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_swipe);

        repository = new SwipeMediaRepository(this);
        cleanupPreferences = new CleanupPreferences(this);
        folderAdapter = new FolderOptionAdapter(buckets -> {
            selectedBucketIds.clear();
            for (BucketOption bucket : buckets) {
                selectedBucketIds.add(bucket.getBucketId());
            }
            updateFolderSummary();
        });
        trashAdapter = new SwipeTrashAdapter(this::toggleTrashSelection);

        BottomNavController.bind(this, BottomNavController.TAB_SWIPE);
        bindViews();
        bindClicks();
        bindRecyclerViews();
        bindCardHolders();
        bindBackHandler();

        if (!PermissionHelper.hasRequiredPermissions(this)) {
            showPermissionState();
            return;
        }
        loadMedia();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!PermissionHelper.hasRequiredPermissions(this)) {
            showPermissionState();
            return;
        }
        if (allMedia.isEmpty()) {
            loadMedia();
        }
    }

    @Override
    protected void onDestroy() {
        stopVideoProgress();
        executor.shutdownNow();
        super.onDestroy();
    }

    private void bindViews() {
        buttonBack = findViewById(R.id.buttonBack);
        buttonUndo = findViewById(R.id.buttonUndo);
        buttonFilter = findViewById(R.id.buttonFilter);
        viewFilterDot = findViewById(R.id.viewFilterDot);
        titleText = findViewById(R.id.textHeaderTitle);
        subtitleText = findViewById(R.id.textHeaderSubtitle);

        swipeStateView = findViewById(R.id.layoutSwipeState);
        emptyStateView = findViewById(R.id.layoutSwipeEmptyState);
        doneStateView = findViewById(R.id.layoutSwipeDoneState);
        trashStateView = findViewById(R.id.layoutTrashState);
        foldersStateView = findViewById(R.id.layoutFoldersState);
        permissionStateView = findViewById(R.id.layoutPermissionState);

        emptyTitleText = findViewById(R.id.textEmptyTitle);
        emptyMessageText = findViewById(R.id.textEmptyMessage);
        emptyActionButton = findViewById(R.id.buttonEmptyAction);
        permissionTitleText = findViewById(R.id.textPermissionTitle);
        permissionMessageText = findViewById(R.id.textPermissionMessage);
        permissionActionButton = findViewById(R.id.buttonPermissionAction);

        frontCardContainer = findViewById(R.id.containerFrontCard);
        backCardContainer = findViewById(R.id.containerBackCard);
        keepLabel = findViewById(R.id.labelKeep);
        deleteLabel = findViewById(R.id.labelDelete);

        swipeActionsView = findViewById(R.id.layoutSwipeActions);
        deleteActionButton = findViewById(R.id.buttonDeleteAction);
        keepActionButton = findViewById(R.id.buttonKeepAction);
        deleteBadgeText = findViewById(R.id.textDeleteBadge);
        keepBadgeText = findViewById(R.id.textKeepBadge);
        deleteBadgeView = findViewById(R.id.layoutDeleteBadge);
        keepBadgeView = findViewById(R.id.layoutKeepBadge);
        continueActionButton = findViewById(R.id.buttonContinueAction);
        doneActionButton = findViewById(R.id.buttonDoneAction);

        trashRecyclerView = findViewById(R.id.recyclerTrash);
        trashSummaryCountText = findViewById(R.id.textTrashSummaryCount);
        trashSummarySizeText = findViewById(R.id.textTrashSummarySize);
        deleteNowButton = findViewById(R.id.buttonDeleteNow);

        foldersRecyclerView = findViewById(R.id.recyclerFolders);
        applyFoldersButton = findViewById(R.id.buttonApplyFolders);

        filterOverlay = findViewById(R.id.layoutFilterOverlay);
        filterSheet = findViewById(R.id.cardFilterSheet);
        filterAllButton = findViewById(R.id.buttonFilterAll);
        filterImagesButton = findViewById(R.id.buttonFilterImages);
        filterVideosButton = findViewById(R.id.buttonFilterVideos);
        sortLargestButton = findViewById(R.id.buttonSortLargest);
        sortSmallestButton = findViewById(R.id.buttonSortSmallest);
        sortNewestButton = findViewById(R.id.buttonSortNewest);
        sortOldestButton = findViewById(R.id.buttonSortOldest);
        foldersSummaryText = findViewById(R.id.textFoldersSummary);

        metadataOverlay = findViewById(R.id.layoutMetadataOverlay);
        metadataSheet = findViewById(R.id.cardMetadataSheet);
        metadataTitleText = findViewById(R.id.textMetadataTitle);
        metadataTypeText = findViewById(R.id.textMetadataType);
        metadataSizeText = findViewById(R.id.textMetadataSize);
        metadataResolutionText = findViewById(R.id.textMetadataResolution);
        metadataAlbumText = findViewById(R.id.textMetadataAlbum);
        metadataDateText = findViewById(R.id.textMetadataDate);
        metadataDurationText = findViewById(R.id.textMetadataDuration);
    }

    private void bindRecyclerViews() {
        trashRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        trashRecyclerView.setAdapter(trashAdapter);

        foldersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        foldersRecyclerView.setAdapter(folderAdapter);
    }

    private void bindCardHolders() {
        LayoutInflater inflater = LayoutInflater.from(this);
        frontCardHolder = new SwipeCardHolder(inflater, frontCardContainer);
        backCardHolder = new SwipeCardHolder(inflater, backCardContainer);

        frontCardContainer.setCallback(new SwipeDragLayout.Callback() {
            @Override
            public boolean canStartSwipe() {
                return screenMode == ScreenMode.SWIPE
                        && !cardAnimating
                        && !filteredMedia.isEmpty()
                        && currentIndex < filteredMedia.size();
            }

            @Override
            public void onSwipeDrag(float translationX, float translationY) {
                applyCardTransform(translationX, translationY);
            }

            @Override
            public void onSwipeRelease(float translationX, float translationY, float velocityX) {
                if (shouldCommitSwipe(translationX, velocityX)) {
                    performDecision(translationX >= 0f, translationX, translationY);
                } else {
                    resetFrontCard();
                }
            }
        });
    }

    private void bindClicks() {
        buttonBack.setOnClickListener(v -> handleBack());
        buttonUndo.setOnClickListener(v -> undoLastDecision());
        buttonFilter.setOnClickListener(v -> showFilterSheet());
        deleteActionButton.setOnClickListener(v -> {
            if (canTakeAction()) {
                performDecision(false, -Math.max(dp(360), frontCardContainer.getWidth()), 0f);
            }
        });
        keepActionButton.setOnClickListener(v -> {
            if (canTakeAction()) {
                performDecision(true, Math.max(dp(360), frontCardContainer.getWidth()), 0f);
            }
        });
        continueActionButton.setOnClickListener(v -> openTrashReview());
        doneActionButton.setOnClickListener(v -> {
            if (getDeletedItems().isEmpty()) {
                openSuccess(0L, 0, history.size());
            } else {
                openTrashReview();
            }
        });
        deleteNowButton.setOnClickListener(v -> beginDeleteFlow());
        applyFoldersButton.setOnClickListener(v -> {
            screenMode = ScreenMode.SWIPE;
            applyFiltersAndReset();
        });
        emptyActionButton.setOnClickListener(v -> {
            if (!PermissionHelper.hasRequiredPermissions(this)) {
                PermissionHelper.openAppSettings(this);
            } else {
                resetFilters();
            }
        });
        permissionActionButton.setOnClickListener(v -> PermissionHelper.openAppSettings(this));

        filterOverlay.setOnClickListener(v -> dismissFilterSheet());
        filterSheet.setOnClickListener(v -> { });
        filterAllButton.setOnClickListener(v -> {
            activeFilter = MediaFilter.ALL;
            applyFiltersAndReset();
            bindFilterControls();
        });
        filterImagesButton.setOnClickListener(v -> {
            activeFilter = MediaFilter.IMAGES;
            applyFiltersAndReset();
            bindFilterControls();
        });
        filterVideosButton.setOnClickListener(v -> {
            activeFilter = MediaFilter.VIDEOS;
            applyFiltersAndReset();
            bindFilterControls();
        });
        sortLargestButton.setOnClickListener(v -> {
            activeSort = SortOrder.SIZE_DESC;
            applyFiltersAndReset();
            bindFilterControls();
        });
        sortSmallestButton.setOnClickListener(v -> {
            activeSort = SortOrder.SIZE_ASC;
            applyFiltersAndReset();
            bindFilterControls();
        });
        sortNewestButton.setOnClickListener(v -> {
            activeSort = SortOrder.TIME_DESC;
            applyFiltersAndReset();
            bindFilterControls();
        });
        sortOldestButton.setOnClickListener(v -> {
            activeSort = SortOrder.TIME_ASC;
            applyFiltersAndReset();
            bindFilterControls();
        });
        findViewById(R.id.buttonSelectFolders).setOnClickListener(v -> {
            dismissFilterSheet();
            screenMode = ScreenMode.FOLDERS;
            bindAll();
        });
        findViewById(R.id.buttonApplyFilters).setOnClickListener(v -> dismissFilterSheet());

        metadataOverlay.setOnClickListener(v -> dismissMetadataSheet());
        metadataSheet.setOnClickListener(v -> { });
        findViewById(R.id.buttonCloseMetadata).setOnClickListener(v -> dismissMetadataSheet());
    }

    private void bindBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
    }

    private void handleBack() {
        if (metadataOverlay.getVisibility() == View.VISIBLE) {
            dismissMetadataSheet();
            return;
        }
        if (filterOverlay.getVisibility() == View.VISIBLE) {
            dismissFilterSheet();
            return;
        }
        if (screenMode == ScreenMode.TRASH || screenMode == ScreenMode.FOLDERS) {
            screenMode = ScreenMode.SWIPE;
            bindAll();
            return;
        }
        finish();
    }

    private void loadMedia() {
        permissionStateView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.GONE);
        executor.execute(() -> {
            List<SwipeMediaItem> media = repository.loadMedia();
            List<BucketOption> buckets = repository.buildBuckets(media);
            runOnUiThread(() -> {
                allMedia.clear();
                allMedia.addAll(media);
                bucketOptions.clear();
                bucketOptions.addAll(buckets);
                folderAdapter.submitList(bucketOptions);
                folderAdapter.setSelectedBucketIds(selectedBucketIds);
                applyFiltersAndReset();
            });
        });
    }

    private void showPermissionState() {
        stopVideoProgress();
        screenMode = ScreenMode.SWIPE;
        swipeStateView.setVisibility(View.GONE);
        doneStateView.setVisibility(View.GONE);
        trashStateView.setVisibility(View.GONE);
        foldersStateView.setVisibility(View.GONE);
        permissionStateView.setVisibility(View.VISIBLE);
        titleText.setText(R.string.swipe_title);
        subtitleText.setText(R.string.permission_required_toast);
        buttonUndo.setVisibility(View.INVISIBLE);
        buttonFilter.setVisibility(View.VISIBLE);
        viewFilterDot.setVisibility(View.GONE);
        permissionTitleText.setText(R.string.photos_access);
        permissionMessageText.setText(R.string.storage_access_message);
        permissionActionButton.setText(R.string.open_settings);
    }

    private void applyFiltersAndReset() {
        stopVideoProgress();
        filteredMedia.clear();
        for (SwipeMediaItem item : allMedia) {
            if (activeFilter == MediaFilter.IMAGES && item.isVideo()) {
                continue;
            }
            if (activeFilter == MediaFilter.VIDEOS && !item.isVideo()) {
                continue;
            }
            if (!selectedBucketIds.isEmpty() && !selectedBucketIds.contains(item.getBucketId())) {
                continue;
            }
            filteredMedia.add(item);
        }
        Collections.sort(filteredMedia, buildComparator());
        history.clear();
        currentIndex = 0;
        trashSelectionKeys.clear();
        playingKey = null;
        selectedMetadataItem = null;
        screenMode = ScreenMode.SWIPE;
        bindAll();
    }

    @NonNull
    private Comparator<SwipeMediaItem> buildComparator() {
        switch (activeSort) {
            case SIZE_ASC:
                return Comparator.comparingLong(SwipeMediaItem::getSizeBytes);
            case TIME_ASC:
                return Comparator.comparingLong(SwipeMediaItem::getDateMillis);
            case TIME_DESC:
                return (first, second) -> Long.compare(second.getDateMillis(), first.getDateMillis());
            case SIZE_DESC:
            default:
                return (first, second) -> Long.compare(second.getSizeBytes(), first.getSizeBytes());
        }
    }

    private void bindAll() {
        bindHeader();
        bindSwipeState();
        bindTrashState();
        bindFolderState();
        bindFilterControls();
        updateFolderSummary();
    }

    private void bindHeader() {
        if (screenMode == ScreenMode.TRASH) {
            titleText.setText(R.string.swipe_trash_title);
            subtitleText.setText(getString(R.string.swipe_trash_subtitle, trashSelectionKeys.size()));
            buttonUndo.setVisibility(View.INVISIBLE);
            buttonFilter.setVisibility(View.INVISIBLE);
        } else if (screenMode == ScreenMode.FOLDERS) {
            titleText.setText(R.string.select_folders_title);
            subtitleText.setText(R.string.select_folders_subtitle);
            buttonUndo.setVisibility(View.INVISIBLE);
            buttonFilter.setVisibility(View.INVISIBLE);
        } else {
            titleText.setText(R.string.swipe_title);
            if (filteredMedia.isEmpty()) {
                subtitleText.setText(R.string.swipe_empty_subtitle);
            } else if (currentIndex >= filteredMedia.size()) {
                subtitleText.setText(R.string.swipe_done_subtitle);
            } else {
                subtitleText.setText(getString(
                        R.string.video_position_count,
                        currentIndex + 1,
                        filteredMedia.size()
                ));
            }
            buttonUndo.setVisibility(currentIndex > 0 ? View.VISIBLE : View.INVISIBLE);
            buttonFilter.setVisibility(View.VISIBLE);
        }
        viewFilterDot.setVisibility(hasActiveFilters() ? View.VISIBLE : View.GONE);
    }

    private void bindSwipeState() {
        boolean showSwipeMode = screenMode == ScreenMode.SWIPE;
        swipeStateView.setVisibility(showSwipeMode ? View.VISIBLE : View.GONE);
        trashStateView.setVisibility(screenMode == ScreenMode.TRASH ? View.VISIBLE : View.GONE);
        foldersStateView.setVisibility(screenMode == ScreenMode.FOLDERS ? View.VISIBLE : View.GONE);
        permissionStateView.setVisibility(View.GONE);

        if (!showSwipeMode) {
            stopVideoProgress();
            return;
        }

        boolean hasMedia = !filteredMedia.isEmpty();
        boolean done = hasMedia && currentIndex >= filteredMedia.size();
        int deletedCount = getDeletedItems().size();

        emptyStateView.setVisibility(hasMedia ? View.GONE : View.VISIBLE);
        doneStateView.setVisibility(done ? View.VISIBLE : View.GONE);
        frontCardContainer.setVisibility(hasMedia && !done ? View.VISIBLE : View.INVISIBLE);
        backCardContainer.setVisibility(hasMedia && !done && currentIndex + 1 < filteredMedia.size()
                ? View.VISIBLE
                : View.INVISIBLE);
        swipeActionsView.setVisibility(hasMedia && !done ? View.VISIBLE : View.GONE);
        continueActionButton.setVisibility(hasMedia && !done ? View.VISIBLE : View.GONE);
        doneActionButton.setVisibility(done ? View.VISIBLE : View.GONE);

        if (!hasMedia) {
            stopVideoProgress();
            emptyTitleText.setText(R.string.swipe_empty_title);
            emptyMessageText.setText(R.string.swipe_empty_message);
            emptyActionButton.setText(R.string.swipe_reset_filters);
            return;
        }

        if (done) {
            stopVideoProgress();
            TextView doneTitle = findViewById(R.id.textDoneTitle);
            TextView doneMessage = findViewById(R.id.textDoneMessage);
            doneTitle.setText(R.string.swipe_done_title);
            doneMessage.setText(R.string.swipe_done_message);
            doneActionButton.setText(getDeletedItems().isEmpty()
                    ? R.string.swipe_finish_review
                    : R.string.swipe_review_trash);
            return;
        }

        bindCard(frontCardHolder, filteredMedia.get(currentIndex), true);
        if (currentIndex + 1 < filteredMedia.size()) {
            bindCard(backCardHolder, filteredMedia.get(currentIndex + 1), false);
        }
        resetCardStackImmediately();

        int keptCount = getKeptCount();
        deleteBadgeView.setVisibility(deletedCount > 0 ? View.VISIBLE : View.GONE);
        keepBadgeView.setVisibility(keptCount > 0 ? View.VISIBLE : View.GONE);
        deleteBadgeText.setText(String.valueOf(deletedCount));
        keepBadgeText.setText(String.valueOf(keptCount));
        continueActionButton.setEnabled(deletedCount > 0);
        continueActionButton.setAlpha(deletedCount > 0 ? 1f : 0.45f);
    }

    private void bindTrashState() {
        if (screenMode != ScreenMode.TRASH) {
            return;
        }
        List<SwipeMediaItem> deletedItems = getDeletedItems();
        trashAdapter.submitList(deletedItems);
        trashAdapter.setSelectedKeys(trashSelectionKeys);

        long selectedBytes = 0L;
        for (SwipeMediaItem item : deletedItems) {
            if (trashSelectionKeys.contains(item.getStableKey())) {
                selectedBytes += item.getSizeBytes();
            }
        }

        trashSummaryCountText.setText(getString(R.string.swipe_trash_count, trashSelectionKeys.size()));
        trashSummarySizeText.setText(getString(R.string.swipe_trash_size, FormatUtils.formatStorage(selectedBytes)));
        deleteNowButton.setEnabled(!deleteInProgress && !trashSelectionKeys.isEmpty());
        deleteNowButton.setText(deleteInProgress ? R.string.delete_in_progress : R.string.swipe_delete_now);
    }

    private void openTrashReview() {
        List<SwipeMediaItem> deletedItems = getDeletedItems();
        if (deletedItems.isEmpty()) {
            return;
        }
        screenMode = ScreenMode.TRASH;
        trashSelectionKeys.clear();
        for (SwipeMediaItem item : deletedItems) {
            trashSelectionKeys.add(item.getStableKey());
        }
        bindAll();
    }

    private void bindFolderState() {
        if (screenMode != ScreenMode.FOLDERS) {
            return;
        }
        folderAdapter.setSelectedBucketIds(selectedBucketIds);
        updateFolderSummary();
    }

    private void updateFolderSummary() {
        if (foldersSummaryText == null) {
            return;
        }
        if (selectedBucketIds.isEmpty()) {
            foldersSummaryText.setText(R.string.swipe_all_folders_selected);
        } else {
            foldersSummaryText.setText(getResources().getQuantityString(
                    R.plurals.swipe_selected_folders_summary,
                    selectedBucketIds.size(),
                    selectedBucketIds.size()
            ));
        }
    }

    private void bindFilterControls() {
        bindPill(filterAllButton, activeFilter == MediaFilter.ALL);
        bindPill(filterImagesButton, activeFilter == MediaFilter.IMAGES);
        bindPill(filterVideosButton, activeFilter == MediaFilter.VIDEOS);
        bindSortButton(sortLargestButton, activeSort == SortOrder.SIZE_DESC);
        bindSortButton(sortSmallestButton, activeSort == SortOrder.SIZE_ASC);
        bindSortButton(sortNewestButton, activeSort == SortOrder.TIME_DESC);
        bindSortButton(sortOldestButton, activeSort == SortOrder.TIME_ASC);
    }

    private void bindPill(@NonNull MaterialButton button, boolean selected) {
        int backgroundColor = getColor(selected ? android.R.color.white : android.R.color.transparent);
        int textColor = getColor(selected ? R.color.color_scan_bg : R.color.color_scan_text_secondary);
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setTextColor(textColor);
    }

    private void bindSortButton(@NonNull MaterialButton button, boolean selected) {
        int backgroundColor = getColor(selected ? R.color.color_scan_gold : android.R.color.transparent);
        int textColor = getColor(selected ? R.color.color_scan_bg : R.color.color_scan_text_secondary);
        int strokeColor = getColor(selected ? R.color.color_scan_gold : R.color.color_scan_border);
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setTextColor(textColor);
        button.setStrokeColor(ColorStateList.valueOf(strokeColor));
        button.setIconTint(ColorStateList.valueOf(textColor));
    }

    private boolean canTakeAction() {
        return screenMode == ScreenMode.SWIPE
                && !filteredMedia.isEmpty()
                && currentIndex < filteredMedia.size()
                && !cardAnimating;
    }

    private boolean shouldCommitSwipe(float translationX, float velocityX) {
        return Math.abs(translationX) > Math.max(frontCardContainer.getWidth() * 0.24f, dp(96))
                || Math.abs(velocityX) > 1000f;
    }

    private void performDecision(boolean keep, float targetX, float targetY) {
        if (!canTakeAction()) {
            return;
        }
        SwipeMediaItem current = filteredMedia.get(currentIndex);
        cardAnimating = true;
        history.add(new SwipeDecision(current.getStableKey(), keep));
        stopVideoProgress();

        frontCardContainer.animate().cancel();
        backCardContainer.animate().cancel();
        frontCardContainer.animate()
                .translationX(targetX)
                .translationY(targetY)
                .rotation(targetX / dp(16))
                .alpha(0f)
                .setDuration(220L)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        frontCardContainer.animate().setListener(null);
                        resetCardStackImmediately();
                        currentIndex++;
                        cardAnimating = false;
                        bindAll();
                    }
                })
                .start();
    }

    private void undoLastDecision() {
        if (history.isEmpty() || currentIndex <= 0 || cardAnimating) {
            return;
        }
        history.remove(history.size() - 1);
        currentIndex--;
        bindAll();
    }

    private void applyCardTransform(float translationX, float translationY) {
        frontCardContainer.setTranslationX(translationX);
        frontCardContainer.setTranslationY(translationY);
        frontCardContainer.setRotation(Math.max(-18f, Math.min(18f, translationX / dp(14))));

        float progress = Math.min(1f, Math.abs(translationX) / dp(160));
        keepLabel.setAlpha(translationX > 0f ? progress : 0f);
        deleteLabel.setAlpha(translationX < 0f ? progress : 0f);

        if (backCardContainer.getVisibility() == View.VISIBLE) {
            float scale = 0.95f + (0.05f * progress);
            backCardContainer.setScaleX(scale);
            backCardContainer.setScaleY(scale);
            backCardContainer.setTranslationY(dp(20) * (1f - progress));
        }
    }

    private void resetFrontCard() {
        frontCardContainer.animate().cancel();
        backCardContainer.animate().cancel();
        frontCardContainer.animate()
                .translationX(0f)
                .translationY(0f)
                .rotation(0f)
                .alpha(1f)
                .setDuration(180L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        keepLabel.animate().alpha(0f).setDuration(140L).start();
        deleteLabel.animate().alpha(0f).setDuration(140L).start();
        backCardContainer.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .translationY(dp(20))
                .setDuration(180L)
                .start();
    }

    private void resetCardStackImmediately() {
        frontCardContainer.animate().cancel();
        backCardContainer.animate().cancel();
        frontCardContainer.setTranslationX(0f);
        frontCardContainer.setTranslationY(0f);
        frontCardContainer.setRotation(0f);
        frontCardContainer.setAlpha(1f);
        backCardContainer.setScaleX(0.95f);
        backCardContainer.setScaleY(0.95f);
        backCardContainer.setTranslationX(0f);
        backCardContainer.setTranslationY(dp(20));
        backCardContainer.setAlpha(1f);
        keepLabel.setAlpha(0f);
        deleteLabel.setAlpha(0f);
    }

    private void bindCard(@NonNull SwipeCardHolder holder, @NonNull SwipeMediaItem item, boolean front) {
        holder.root.setVisibility(View.VISIBLE);
        Glide.with(holder.image)
                .load(item.getUri())
                .centerCrop()
                .into(holder.image);

        holder.bestShotChip.setVisibility(item.isBestShot() && !item.isVideo() ? View.VISIBLE : View.GONE);
        holder.typeIcon.setImageResource(item.isVideo() ? R.drawable.ic_video : R.drawable.ic_photo);
        holder.typeText.setText(item.isVideo() ? R.string.swipe_type_video : R.string.swipe_type_photo);
        holder.bucketText.setText(item.getBucketName());
        holder.dateText.setText(formatRelativeDate(item.getDateMillis()));
        holder.sizeText.setText(FormatUtils.formatStorage(item.getSizeBytes()));
        holder.infoButton.setOnClickListener(v -> showMetadata(item));

        if (item.isVideo()) {
            holder.playButton.setVisibility(View.VISIBLE);
            holder.progressTrack.setVisibility(front && item.getStableKey().equals(playingKey) ? View.VISIBLE : View.GONE);
            holder.playButton.setImageResource(item.getStableKey().equals(playingKey)
                    ? android.R.drawable.ic_media_pause
                    : R.drawable.ic_play_filled);
            holder.playButton.setOnClickListener(v -> toggleVideoPlayback(item, holder));
        } else {
            holder.playButton.setVisibility(View.GONE);
            holder.progressTrack.setVisibility(View.GONE);
            holder.playButton.setOnClickListener(null);
        }

        if (!front) {
            holder.progressTrack.setVisibility(View.GONE);
        }

        holder.root.setScaleX(front ? 1f : 0.95f);
        holder.root.setScaleY(front ? 1f : 0.95f);
        holder.root.setTranslationY(front ? 0f : dp(20));
        holder.root.setTranslationX(0f);
        holder.root.setRotation(0f);
        holder.root.setAlpha(1f);
    }

    private void toggleVideoPlayback(@NonNull SwipeMediaItem item, @NonNull SwipeCardHolder holder) {
        if (!item.isVideo()) {
            return;
        }
        if (item.getStableKey().equals(playingKey)) {
            playingKey = null;
            stopVideoProgress();
            bindAll();
            return;
        }
        playingKey = item.getStableKey();
        startVideoProgress(holder, item);
        bindAll();
    }

    private void startVideoProgress(@NonNull SwipeCardHolder holder, @NonNull SwipeMediaItem item) {
        stopVideoProgress();
        holder.progressTrack.setVisibility(View.VISIBLE);
        holder.progressFill.setScaleX(0f);
        videoProgressAnimator = ValueAnimator.ofFloat(0f, 1f);
        videoProgressAnimator.setDuration(VIDEO_PROGRESS_DURATION_MS);
        videoProgressAnimator.addUpdateListener(animation ->
                holder.progressFill.setScaleX((float) animation.getAnimatedValue()));
        videoProgressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (item.getStableKey().equals(playingKey)) {
                    playingKey = null;
                    bindAll();
                }
            }
        });
        videoProgressAnimator.start();
    }

    private void stopVideoProgress() {
        if (videoProgressAnimator != null) {
            videoProgressAnimator.cancel();
            videoProgressAnimator = null;
        }
    }

    private void showFilterSheet() {
        filterOverlay.setVisibility(View.VISIBLE);
        filterOverlay.setAlpha(0f);
        filterSheet.post(() -> {
            filterSheet.setTranslationY(filterSheet.getHeight());
            filterOverlay.animate().alpha(1f).setDuration(180L).start();
            filterSheet.animate().translationY(0f).setDuration(220L).start();
        });
    }

    private void dismissFilterSheet() {
        if (filterOverlay.getVisibility() != View.VISIBLE) {
            return;
        }
        filterOverlay.animate()
                .alpha(0f)
                .setDuration(160L)
                .withEndAction(() -> {
                    filterOverlay.setVisibility(View.GONE);
                    filterOverlay.setAlpha(1f);
                    filterSheet.setTranslationY(0f);
                })
                .start();
        filterSheet.animate().translationY(filterSheet.getHeight()).setDuration(200L).start();
    }

    private void showMetadata(@NonNull SwipeMediaItem item) {
        selectedMetadataItem = item;
        metadataTitleText.setText(item.getDisplayName());
        metadataTypeText.setText(item.isVideo() ? getString(R.string.swipe_type_video) : getString(R.string.swipe_type_photo));
        metadataSizeText.setText(FormatUtils.formatStorage(item.getSizeBytes()));
        metadataResolutionText.setText(getString(R.string.swipe_resolution_value, item.getWidth(), item.getHeight()));
        metadataAlbumText.setText(item.getBucketName());
        metadataDateText.setText(FormatUtils.formatShortDate(item.getDateMillis()));
        metadataDurationText.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);
        if (item.isVideo()) {
            metadataDurationText.setText(getString(R.string.swipe_duration_value, FormatUtils.formatDuration(item.getDurationMs())));
        }

        metadataOverlay.setVisibility(View.VISIBLE);
        metadataOverlay.setAlpha(0f);
        metadataSheet.post(() -> {
            metadataSheet.setTranslationY(metadataSheet.getHeight());
            metadataOverlay.animate().alpha(1f).setDuration(180L).start();
            metadataSheet.animate().translationY(0f).setDuration(220L).start();
        });
    }

    private void dismissMetadataSheet() {
        selectedMetadataItem = null;
        if (metadataOverlay.getVisibility() != View.VISIBLE) {
            return;
        }
        metadataOverlay.animate()
                .alpha(0f)
                .setDuration(160L)
                .withEndAction(() -> {
                    metadataOverlay.setVisibility(View.GONE);
                    metadataOverlay.setAlpha(1f);
                    metadataSheet.setTranslationY(0f);
                })
                .start();
        metadataSheet.animate().translationY(metadataSheet.getHeight()).setDuration(200L).start();
    }

    private void toggleTrashSelection(@NonNull SwipeMediaItem item) {
        if (trashSelectionKeys.contains(item.getStableKey())) {
            trashSelectionKeys.remove(item.getStableKey());
        } else {
            trashSelectionKeys.add(item.getStableKey());
        }
        bindAll();
    }

    private void beginDeleteFlow() {
        if (deleteInProgress) {
            return;
        }
        List<SwipeMediaItem> selectedItems = new ArrayList<>();
        for (SwipeMediaItem item : getDeletedItems()) {
            if (trashSelectionKeys.contains(item.getStableKey())) {
                selectedItems.add(item);
            }
        }
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, R.string.select_to_delete, Toast.LENGTH_SHORT).show();
            return;
        }

        deleteInProgress = true;
        pendingDeleteItems.clear();
        pendingDeleteItems.addAll(selectedItems);
        bindAll();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            launchDeleteRequest(selectedItems);
        } else {
            deleteLegacyItems(selectedItems);
        }
    }

    private void launchDeleteRequest(@NonNull List<SwipeMediaItem> items) {
        List<android.net.Uri> uris = new ArrayList<>();
        for (SwipeMediaItem item : items) {
            uris.add(item.getUri());
        }
        PendingIntent pendingIntent = MediaStore.createDeleteRequest(getContentResolver(), uris);
        deleteRequestLauncher.launch(new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build());
    }

    private void verifyDeleteRequestOutcome(@NonNull List<SwipeMediaItem> items) {
        executor.execute(() -> {
            int deletedCount = 0;
            long freedBytes = 0L;
            for (SwipeMediaItem item : items) {
                if (!stillExists(item)) {
                    deletedCount++;
                    freedBytes += item.getSizeBytes();
                }
            }
            int finalDeletedCount = deletedCount;
            long finalFreedBytes = freedBytes;
            int finalFailedCount = items.size() - deletedCount;
            runOnUiThread(() -> finishDeleteFlow(finalFreedBytes, finalDeletedCount, finalFailedCount));
        });
    }

    private void deleteLegacyItems(@NonNull List<SwipeMediaItem> items) {
        executor.execute(() -> {
            int deletedCount = 0;
            int failedCount = 0;
            long freedBytes = 0L;
            for (SwipeMediaItem item : items) {
                try {
                    int rows = getContentResolver().delete(item.getUri(), null, null);
                    if (rows > 0 || !stillExists(item)) {
                        deletedCount++;
                        freedBytes += item.getSizeBytes();
                    } else {
                        failedCount++;
                    }
                } catch (RecoverableSecurityException exception) {
                    runOnUiThread(() -> launchRecoverableDelete(exception));
                    return;
                } catch (Exception exception) {
                    failedCount++;
                }
            }
            int finalDeletedCount = deletedCount;
            long finalFreedBytes = freedBytes;
            int finalFailedCount = failedCount;
            runOnUiThread(() -> finishDeleteFlow(finalFreedBytes, finalDeletedCount, finalFailedCount));
        });
    }

    private void launchRecoverableDelete(@NonNull RecoverableSecurityException exception) {
        IntentSender intentSender = exception.getUserAction().getActionIntent().getIntentSender();
        deleteRequestLauncher.launch(new IntentSenderRequest.Builder(intentSender).build());
    }

    private boolean stillExists(@NonNull SwipeMediaItem item) {
        try (Cursor cursor = getContentResolver().query(
                item.getUri(),
                new String[]{MediaStore.MediaColumns._ID},
                null,
                null,
                null
        )) {
            return cursor != null && cursor.moveToFirst();
        } catch (Exception exception) {
            return false;
        }
    }

    private void finishDeleteFlow(long freedBytes, int deletedCount, int failedCount) {
        deleteInProgress = false;
        pendingDeleteItems.clear();
        if (deletedCount <= 0) {
            Toast.makeText(this, R.string.delete_failed_message, Toast.LENGTH_LONG).show();
            bindAll();
            return;
        }

        cleanupPreferences.addCleanupHistory(new CleanupHistoryEntry(
                System.currentTimeMillis(),
                deletedCount,
                freedBytes
        ));
        if (failedCount > 0) {
            Toast.makeText(this, getString(R.string.partial_delete_message, failedCount), Toast.LENGTH_LONG).show();
        }
        openSuccess(freedBytes, deletedCount, history.size());
    }

    private void openSuccess(long freedBytes, int deletedCount, int reviewedCount) {
        Intent intent = new Intent(this, SwipeSuccessActivity.class);
        intent.putExtra(SwipeSuccessActivity.EXTRA_FREED_BYTES, freedBytes);
        intent.putExtra(SwipeSuccessActivity.EXTRA_DELETED_COUNT, deletedCount);
        intent.putExtra(SwipeSuccessActivity.EXTRA_REVIEWED_COUNT, reviewedCount);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void resetFilters() {
        activeFilter = MediaFilter.ALL;
        activeSort = SortOrder.SIZE_DESC;
        selectedBucketIds.clear();
        folderAdapter.setSelectedBucketIds(selectedBucketIds);
        applyFiltersAndReset();
    }

    private boolean hasActiveFilters() {
        return activeFilter != MediaFilter.ALL
                || activeSort != SortOrder.SIZE_DESC
                || !selectedBucketIds.isEmpty();
    }

    @NonNull
    private List<SwipeMediaItem> getDeletedItems() {
        Map<String, SwipeMediaItem> byKey = new LinkedHashMap<>();
        for (SwipeMediaItem item : filteredMedia) {
            byKey.put(item.getStableKey(), item);
        }
        List<SwipeMediaItem> items = new ArrayList<>();
        for (SwipeDecision decision : history) {
            if (!decision.keep) {
                SwipeMediaItem item = byKey.get(decision.key);
                if (item != null) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    private int getKeptCount() {
        int keptCount = 0;
        for (SwipeDecision decision : history) {
            if (decision.keep) {
                keptCount++;
            }
        }
        return keptCount;
    }

    private String formatRelativeDate(long timeMs) {
        long diffDays = Math.max(0L, (System.currentTimeMillis() - timeMs) / (24L * 60L * 60L * 1000L));
        if (diffDays == 0L) {
            return getString(R.string.swipe_today);
        }
        if (diffDays == 1L) {
            return getString(R.string.swipe_yesterday);
        }
        if (diffDays < 7L) {
            return getString(R.string.swipe_last_week);
        }
        if (diffDays < 31L) {
            return getString(R.string.swipe_last_month);
        }
        return FormatUtils.formatShortDate(timeMs);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private static final class SwipeDecision {
        final String key;
        final boolean keep;

        SwipeDecision(@NonNull String key, boolean keep) {
            this.key = key;
            this.keep = keep;
        }
    }

    private static final class SwipeCardHolder {

        final View root;
        final ImageView image;
        final View bestShotChip;
        final ImageView typeIcon;
        final TextView typeText;
        final TextView bucketText;
        final TextView dateText;
        final TextView sizeText;
        final ImageButton infoButton;
        final ImageButton playButton;
        final View progressTrack;
        final View progressFill;

        SwipeCardHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
            root = inflater.inflate(R.layout.layout_swipe_media_card, parent, false);
            parent.removeAllViews();
            parent.addView(root);
            image = root.findViewById(R.id.imageCardPreview);
            bestShotChip = root.findViewById(R.id.layoutBestShotChip);
            typeIcon = root.findViewById(R.id.imageTypeIcon);
            typeText = root.findViewById(R.id.textTypeLabel);
            bucketText = root.findViewById(R.id.textBucketName);
            dateText = root.findViewById(R.id.textDateLabel);
            sizeText = root.findViewById(R.id.textSizeValue);
            infoButton = root.findViewById(R.id.buttonInfo);
            playButton = root.findViewById(R.id.buttonPlay);
            progressTrack = root.findViewById(R.id.viewVideoProgressTrack);
            progressFill = root.findViewById(R.id.viewVideoProgressFill);
            progressFill.setPivotX(0f);
        }
    }
}
