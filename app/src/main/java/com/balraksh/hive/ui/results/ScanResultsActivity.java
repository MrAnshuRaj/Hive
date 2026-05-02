package com.balraksh.hive.ui.results;

import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.balraksh.hive.R;
import com.balraksh.hive.adapters.ResultGroupAdapter;
import com.balraksh.hive.data.CleanupHistoryEntry;
import com.balraksh.hive.data.CleanupOutcome;
import com.balraksh.hive.data.MediaGroup;
import com.balraksh.hive.data.MediaImageItem;
import com.balraksh.hive.data.ScanResult;
import com.balraksh.hive.repository.CleanupPreferences;
import com.balraksh.hive.repository.ScanSessionStore;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.cleanup.CleanupSuccessActivity;
import com.balraksh.hive.utils.FormatUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanResultsActivity extends BaseEdgeToEdgeActivity {

    public static final String EXTRA_INITIAL_TAB_INDEX = "extra_initial_tab_index";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ResultGroupAdapter adapter;
    private ScanResult scanResult;
    private TextView headerTitleText;
    private TextView headerSubtitleText;
    private TextView selectedSpaceText;
    private View emptyStateView;
    private View actionPanel;
    private MaterialButton deleteButton;
    private List<MediaGroup> displayGroups = Collections.emptyList();
    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;
    private List<MediaImageItem> pendingDeleteItems = new ArrayList<>();
    private boolean deleteInProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_scan_results);

        scanResult = ScanSessionStore.getInstance().getCurrentResult();
        if (scanResult == null) {
            finish();
            return;
        }

        deleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            verifyDeleteRequestOutcome(pendingDeleteItems);
                        } else {
                            deleteLegacyItems(pendingDeleteItems);
                        }
                    } else {
                        deleteInProgress = false;
                        refreshUi();
                    }
                }
        );

        headerTitleText = findViewById(R.id.textHeaderTitle);
        headerSubtitleText = findViewById(R.id.textHeaderSubtitle);
        selectedSpaceText = findViewById(R.id.textSelectedSpaceValue);
        emptyStateView = findViewById(R.id.layoutEmptyState);
        actionPanel = findViewById(R.id.cardActionPanel);
        deleteButton = findViewById(R.id.buttonDeleteSelected);

        RecyclerView recyclerView = findViewById(R.id.recyclerGroups);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResultGroupAdapter(this::refreshSelectionSummary);
        recyclerView.setAdapter(adapter);

        int initialTabIndex = getIntent().getIntExtra(EXTRA_INITIAL_TAB_INDEX, 0);
        displayGroups = buildDisplayGroups(initialTabIndex);

        deleteButton.setOnClickListener(v -> beginDeleteFlow());

        refreshUi();
        animateEntrance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void refreshUi() {
        if (scanResult == null) {
            return;
        }

        List<MediaGroup> groups = displayGroups == null ? Collections.emptyList() : displayGroups;
        adapter.setGroups(groups);
        emptyStateView.setVisibility(groups.isEmpty() ? View.VISIBLE : View.GONE);
        actionPanel.setVisibility(groups.isEmpty() ? View.GONE : View.VISIBLE);

        headerTitleText.setText(resolveHeaderTitle());
        headerSubtitleText.setText(R.string.clean_duplicates_subtitle);
        refreshSelectionSummary();
    }

    private void refreshSelectionSummary() {
        if (scanResult == null) {
            return;
        }
        long selectedBytes = ScanSessionStore.getInstance().getSelectedBytes();
        int selectedCount = ScanSessionStore.getInstance().getSelectedCount();
        selectedSpaceText.setText(FormatUtils.formatStorage(selectedBytes));
        deleteButton.setEnabled(!deleteInProgress && selectedCount > 0);
        deleteButton.setAlpha(!deleteInProgress && selectedCount > 0 ? 1f : 0.58f);
        deleteButton.setText(deleteInProgress
                ? getString(R.string.delete_in_progress)
                : (selectedCount > 0 ? getString(R.string.clean_now) : getString(R.string.select_to_delete)));

        CleanupPreferences cleanupPreferences = new CleanupPreferences(this);
        cleanupPreferences.setLastScanPotentialBytes(selectedBytes);
        cleanupPreferences.setLastScanInsights(
                scanResult.getDuplicateMatchCount(),
                calculatePotentialBytes(scanResult.getDuplicateGroups()),
                scanResult.getSimilarMatchCount(),
                calculatePotentialBytes(scanResult.getSimilarGroups())
        );
    }

    private void beginDeleteFlow() {
        List<MediaImageItem> selectedItems = ScanSessionStore.getInstance().getSelectedItems();
        if (selectedItems.isEmpty()) {
            refreshUi();
            return;
        }

        deleteInProgress = true;
        pendingDeleteItems = new ArrayList<>(selectedItems);
        refreshUi();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            launchDeleteRequest(selectedItems);
        } else {
            deleteLegacyItems(selectedItems);
        }
    }

    private void launchDeleteRequest(List<MediaImageItem> items) {
        List<Uri> uris = new ArrayList<>();
        for (MediaImageItem item : items) {
            uris.add(item.getUri());
        }
        PendingIntent pendingIntent = MediaStore.createDeleteRequest(getContentResolver(), uris);
        deleteRequestLauncher.launch(
                new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build()
        );
    }

    private void verifyDeleteRequestOutcome(List<MediaImageItem> items) {
        executor.execute(() -> {
            int deletedCount = 0;
            long freedBytes = 0L;
            for (MediaImageItem item : items) {
                if (!stillExists(item.getUri())) {
                    deletedCount++;
                    freedBytes += item.getSizeBytes();
                }
            }
            CleanupOutcome outcome = new CleanupOutcome(
                    deletedCount,
                    Math.max(0, items.size() - deletedCount),
                    freedBytes
            );
            runOnUiThread(() -> finishDeleteFlow(outcome));
        });
    }

    private void deleteLegacyItems(List<MediaImageItem> items) {
        pendingDeleteItems = new ArrayList<>(items);
        executor.execute(() -> {
            int deletedCount = 0;
            int failedCount = 0;
            long freedBytes = 0L;

            for (MediaImageItem item : items) {
                try {
                    int rows = getContentResolver().delete(item.getUri(), null, null);
                    if (rows > 0 || !stillExists(item.getUri())) {
                        deletedCount++;
                        freedBytes += item.getSizeBytes();
                    } else {
                        failedCount++;
                    }
                } catch (RecoverableSecurityException exception) {
                    pendingDeleteItems = items;
                    runOnUiThread(() -> launchRecoverableDelete(exception));
                    return;
                } catch (Exception exception) {
                    failedCount++;
                }
            }

            CleanupOutcome outcome = new CleanupOutcome(deletedCount, failedCount, freedBytes);
            runOnUiThread(() -> finishDeleteFlow(outcome));
        });
    }

    private void launchRecoverableDelete(RecoverableSecurityException exception) {
        deleteRequestLauncher.launch(
                new IntentSenderRequest.Builder(
                        exception.getUserAction().getActionIntent().getIntentSender()
                ).build()
        );
    }

    private boolean stillExists(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.MediaColumns._ID}, null, null, null)) {
            return cursor != null && cursor.moveToFirst();
        } catch (Exception exception) {
            return false;
        }
    }

    private void finishDeleteFlow(CleanupOutcome outcome) {
        deleteInProgress = false;
        if (outcome.getDeletedCount() == 0) {
            Toast.makeText(this, R.string.delete_failed_message, Toast.LENGTH_LONG).show();
            refreshUi();
            return;
        }

        CleanupPreferences preferences = new CleanupPreferences(this);
        preferences.addCleanupHistory(new CleanupHistoryEntry(
                System.currentTimeMillis(),
                outcome.getDeletedCount(),
                outcome.getFreedBytes()
        ));
        preferences.setLastScanPotentialBytes(0L);

        ScanSessionStore.getInstance().setLastOutcome(outcome);
        ScanSessionStore.getInstance().clearCurrentResult();

        if (outcome.getFailedCount() > 0) {
            Toast.makeText(this, getString(R.string.partial_delete_message, outcome.getFailedCount()), Toast.LENGTH_LONG).show();
        }

        Intent intent = new Intent(this, CleanupSuccessActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private List<MediaGroup> buildDisplayGroups(int initialTabIndex) {
        List<MediaGroup> primaryGroups = new ArrayList<>();
        List<MediaGroup> secondaryGroups = new ArrayList<>();
        if (initialTabIndex == 1) {
            primaryGroups.addAll(scanResult.getSimilarGroups());
            secondaryGroups.addAll(scanResult.getDuplicateGroups());
        } else {
            primaryGroups.addAll(scanResult.getDuplicateGroups());
            secondaryGroups.addAll(scanResult.getSimilarGroups());
        }

        Comparator<MediaGroup> comparator = (first, second) -> Long.compare(
                getGroupTimestamp(second),
                getGroupTimestamp(first)
        );
        primaryGroups.sort(comparator);
        secondaryGroups.sort(comparator);

        List<MediaGroup> merged = new ArrayList<>(primaryGroups.size() + secondaryGroups.size());
        merged.addAll(primaryGroups);
        merged.addAll(secondaryGroups);
        return merged;
    }

    private long getGroupTimestamp(@Nullable MediaGroup group) {
        MediaImageItem bestItem = group == null ? null : group.getBestItem();
        return bestItem == null ? 0L : bestItem.getDateTaken();
    }

    private int resolveHeaderTitle() {
        return scanResult.getDuplicateGroups().isEmpty() && !scanResult.getSimilarGroups().isEmpty()
                ? R.string.clean_similar_title
                : R.string.clean_duplicates_title;
    }

    private void animateEntrance() {
        animateIn(findViewById(R.id.textHeaderTitle), 0L, 22f);
        animateIn(findViewById(R.id.textHeaderSubtitle), 70L, 18f);
        animateIn(findViewById(R.id.recyclerGroups), 130L, 32f);
        animateIn(actionPanel, 220L, 36f);
    }

    private void animateIn(@Nullable View view, long delay, float translationY) {
        if (view == null) {
            return;
        }
        view.setAlpha(0f);
        view.setTranslationY(translationY);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(280L)
                .start();
    }

    private long calculatePotentialBytes(List<? extends MediaGroup> groups) {
        long total = 0L;
        for (MediaGroup group : groups) {
            for (MediaImageItem item : group.getItems()) {
                if (item.getId() != group.getBestItemId()) {
                    total += item.getSizeBytes();
                }
            }
        }
        return total;
    }
}
