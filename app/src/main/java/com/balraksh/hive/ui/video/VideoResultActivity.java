package com.balraksh.hive.ui.video;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.balraksh.hive.R;
import com.balraksh.hive.adapters.CompressionReviewAdapter;
import com.balraksh.hive.data.VideoCompressionHistoryEntry;
import com.balraksh.hive.repository.VideoCompressionPreferences;
import com.balraksh.hive.repository.VideoCompressionSessionStore;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.BottomNavController;
import com.balraksh.hive.utils.FormatUtils;
import com.balraksh.hive.video.VideoCompressionResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoResultActivity extends BaseEdgeToEdgeActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Uri> pendingDeleteUris = new ArrayList<>();
    private long pendingFreedBytes;

    private VideoCompressionSessionStore sessionStore;
    private List<VideoCompressionResult> results;
    private List<VideoCompressionResult> successfulResults;
    private CompressionReviewAdapter adapter;
    private final Map<String, CompressionReviewAdapter.VideoAction> actionMap = new LinkedHashMap<>();

    private TextView bannerTitleView;
    private TextView bannerMessageView;
    private ImageView bannerIconView;
    private TextView totalSavedView;
    private TextView actionCountsView;
    private com.google.android.material.button.MaterialButton applyButton;
    private boolean isApplying;

    private final ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    finalizeAppliedChanges(pendingFreedBytes);
                } else {
                    Toast.makeText(this, R.string.video_apply_failed, Toast.LENGTH_SHORT).show();
                    setApplying(false);
                }
                pendingDeleteUris.clear();
                pendingFreedBytes = 0L;
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_video_result);

        sessionStore = VideoCompressionSessionStore.getInstance(this);
        results = sessionStore.getResults();
        successfulResults = getSuccessfulResults(results);
        if (results.isEmpty()) {
            finish();
            return;
        }

        BottomNavController.bind(this, BottomNavController.TAB_COMPRESS);
        bindViews();
        bindResults();
        bindBanner();
        bindSummary();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void bindViews() {
        bannerTitleView = findViewById(R.id.textBannerTitle);
        bannerMessageView = findViewById(R.id.textBannerMessage);
        bannerIconView = findViewById(R.id.imageBannerIcon);
        totalSavedView = findViewById(R.id.textTotalSavedValue);
        actionCountsView = findViewById(R.id.textActionCounts);
        applyButton = findViewById(R.id.buttonApplyChanges);

        adapter = new CompressionReviewAdapter(new CompressionReviewAdapter.Listener() {
            @Override
            public void onPlayClicked(@NonNull VideoCompressionResult result) {
                playResult(result);
            }

            @Override
            public void onActionSelected(@NonNull VideoCompressionResult result, @NonNull CompressionReviewAdapter.VideoAction action) {
                actionMap.put(result.getSourceUri().toString(), action);
                adapter.setActionMap(actionMap);
                bindSummary();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerResults);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        applyButton.setOnClickListener(v -> applyChanges());
    }

    private void bindResults() {
        for (VideoCompressionResult result : successfulResults) {
            actionMap.put(result.getSourceUri().toString(), CompressionReviewAdapter.VideoAction.REPLACE);
        }
        adapter.submitList(new ArrayList<>(results));
        adapter.setActionMap(actionMap);
    }

    private void bindBanner() {
        if (successfulResults.isEmpty()) {
            bannerIconView.setBackgroundResource(R.drawable.bg_compress_button_circle);
            bannerIconView.setImageResource(R.drawable.ic_warning);
            bannerIconView.setColorFilter(getColor(R.color.color_scan_gold));
            bannerTitleView.setText(R.string.video_all_failed);
            bannerMessageView.setText(R.string.video_no_review_results);
            return;
        }

        bannerIconView.setBackgroundResource(R.drawable.bg_compress_button_circle_active);
        bannerIconView.setImageResource(R.drawable.ic_check_small);
        bannerIconView.setColorFilter(getColor(R.color.color_scan_bg));
        if (successfulResults.size() < results.size()) {
            bannerTitleView.setText(getString(R.string.video_partial_success, successfulResults.size(), results.size()));
        } else {
            bannerTitleView.setText(R.string.video_review_complete_title);
        }
        bannerMessageView.setText(R.string.video_review_complete_message);
    }

    private void bindSummary() {
        long actualSavings = 0L;
        int replaceCount = 0;
        int keepCount = 0;
        int deleteCount = 0;
        for (VideoCompressionResult result : successfulResults) {
            CompressionReviewAdapter.VideoAction action = actionMap.get(result.getSourceUri().toString());
            if (action == CompressionReviewAdapter.VideoAction.REPLACE) {
                replaceCount++;
                actualSavings += result.getSavedBytes();
            } else if (action == CompressionReviewAdapter.VideoAction.KEEP_BOTH) {
                keepCount++;
            } else if (action == CompressionReviewAdapter.VideoAction.DELETE) {
                deleteCount++;
            }
        }

        totalSavedView.setText(FormatUtils.formatStorage(actualSavings));
        actionCountsView.setText(buildActionCounts(replaceCount, keepCount, deleteCount));
        applyButton.setText(successfulResults.isEmpty() ? R.string.compress_more : R.string.video_apply_changes);
        applyButton.setEnabled(!isApplying);
    }

    private void applyChanges() {
        if (isApplying) {
            return;
        }
        if (successfulResults.isEmpty()) {
            sessionStore.resetSelection();
            openSelectAgain();
            return;
        }

        setApplying(true);
        Set<Uri> deleteUriSet = new LinkedHashSet<>();
        pendingFreedBytes = 0L;
        for (VideoCompressionResult result : successfulResults) {
            CompressionReviewAdapter.VideoAction action = resolveAction(result);
            if (action == CompressionReviewAdapter.VideoAction.REPLACE) {
                deleteUriSet.add(result.getSourceUri());
                pendingFreedBytes += result.getSavedBytes();
            } else if (action == CompressionReviewAdapter.VideoAction.DELETE && result.getOutputUri() != null) {
                deleteUriSet.add(result.getOutputUri());
            }
        }
        pendingDeleteUris.clear();
        pendingDeleteUris.addAll(deleteUriSet);

        if (pendingDeleteUris.isEmpty()) {
            finalizeAppliedChanges(pendingFreedBytes);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                PendingIntent pendingIntent = android.provider.MediaStore.createDeleteRequest(
                        getContentResolver(),
                        new ArrayList<>(pendingDeleteUris)
                );
                deleteRequestLauncher.launch(new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build());
            } catch (Exception exception) {
                Toast.makeText(this, R.string.video_apply_failed, Toast.LENGTH_SHORT).show();
                pendingDeleteUris.clear();
                pendingFreedBytes = 0L;
                setApplying(false);
            }
            return;
        }

        executor.execute(() -> {
            long actualFreedBytes = 0L;
            boolean hadError = false;
            for (VideoCompressionResult result : successfulResults) {
                CompressionReviewAdapter.VideoAction action = resolveAction(result);
                if (action == CompressionReviewAdapter.VideoAction.REPLACE) {
                    boolean deleted = deleteUri(result.getSourceUri());
                    if (deleted) {
                        actualFreedBytes += result.getSavedBytes();
                    } else {
                        hadError = true;
                    }
                } else if (action == CompressionReviewAdapter.VideoAction.DELETE && result.getOutputUri() != null) {
                    if (!deleteUri(result.getOutputUri())) {
                        hadError = true;
                    }
                }
            }
            boolean finalHadError = hadError;
            long finalActualFreedBytes = actualFreedBytes;
            runOnUiThread(() -> {
                if (finalHadError) {
                    Toast.makeText(this, R.string.video_apply_failed, Toast.LENGTH_SHORT).show();
                }
                finalizeAppliedChanges(finalActualFreedBytes);
            });
        });
    }

    @NonNull
    private CompressionReviewAdapter.VideoAction resolveAction(@NonNull VideoCompressionResult result) {
        CompressionReviewAdapter.VideoAction action = actionMap.get(result.getSourceUri().toString());
        return action == null ? CompressionReviewAdapter.VideoAction.REPLACE : action;
    }

    private boolean deleteUri(@NonNull Uri uri) {
        try {
            return getContentResolver().delete(uri, null, null) > 0;
        } catch (SecurityException exception) {
            return false;
        }
    }

    private void finalizeAppliedChanges(long freedBytes) {
        setApplying(false);
        int successfulCount = successfulResults.size();
        long totalDurationMs = 0L;
        for (VideoCompressionResult result : successfulResults) {
            totalDurationMs += Math.max(0L, result.getDurationMs());
        }
        persistHistoryIfNeeded(freedBytes);
        sessionStore.resetSelection();
        Intent intent = new Intent(this, VideoSuccessActivity.class);
        intent.putExtra(VideoSuccessActivity.EXTRA_FREED_BYTES, freedBytes);
        intent.putExtra(VideoSuccessActivity.EXTRA_VIDEO_COUNT, successfulCount);
        intent.putExtra(VideoSuccessActivity.EXTRA_TOTAL_DURATION_MS, totalDurationMs);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void setApplying(boolean applying) {
        isApplying = applying;
        if (applyButton != null) {
            applyButton.setEnabled(!applying);
        }
    }

    private void playResult(@NonNull VideoCompressionResult result) {
        Uri uri = result.isSuccess() && result.getOutputUri() != null
                ? result.getOutputUri()
                : result.getSourceUri();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.play)));
    }

    private void openSelectAgain() {
        Intent intent = new Intent(this, VideoSelectActivity.class);
        intent.putExtra(VideoSelectActivity.EXTRA_RESET_SELECTION, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @NonNull
    private List<VideoCompressionResult> getSuccessfulResults(@NonNull List<VideoCompressionResult> allResults) {
        List<VideoCompressionResult> success = new ArrayList<>();
        for (VideoCompressionResult result : allResults) {
            if (result.isSuccess()) {
                success.add(result);
            }
        }
        return success;
    }

    private void persistHistoryIfNeeded(long freedBytes) {
        if (sessionStore.isHistoryRecorded() || successfulResults.isEmpty()) {
            return;
        }
        new VideoCompressionPreferences(this).addHistory(new VideoCompressionHistoryEntry(
                System.currentTimeMillis(),
                successfulResults.size(),
                freedBytes
        ));
        sessionStore.setHistoryRecorded(true);
    }

    @NonNull
    private String buildActionCounts(int replaceCount, int keepCount, int deleteCount) {
        List<String> parts = new ArrayList<>();
        if (replaceCount > 0) {
            parts.add(getString(R.string.video_replace_count, replaceCount));
        }
        if (keepCount > 0) {
            parts.add(getString(R.string.video_keep_count, keepCount));
        }
        if (deleteCount > 0) {
            parts.add(getString(R.string.video_delete_count, deleteCount));
        }
        if (parts.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append("  ");
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }
}
