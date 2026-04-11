package com.balraksh.safkaro.ui.confirm;

import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.data.CleanupHistoryEntry;
import com.balraksh.safkaro.data.CleanupOutcome;
import com.balraksh.safkaro.data.MediaImageItem;
import com.balraksh.safkaro.repository.CleanupPreferences;
import com.balraksh.safkaro.repository.ScanSessionStore;
import com.balraksh.safkaro.ui.BaseEdgeToEdgeActivity;
import com.balraksh.safkaro.ui.cleanup.CleanupSuccessActivity;
import com.balraksh.safkaro.utils.FormatUtils;

public class DeleteConfirmActivity extends BaseEdgeToEdgeActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;
    private List<MediaImageItem> selectedItems = new ArrayList<>();
    private List<MediaImageItem> pendingLegacyItems = new ArrayList<>();
    private MaterialButton deleteButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_delete_confirm);

        selectedItems = ScanSessionStore.getInstance().getSelectedItems();
        if (selectedItems.isEmpty()) {
            finish();
            return;
        }

        deleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            verifyDeleteRequestOutcome(selectedItems);
                        } else {
                            deleteLegacyItems(pendingLegacyItems);
                        }
                    } else {
                        setDeleteButtonEnabled(true);
                    }
                }
        );

        TextView filesValueText = findViewById(R.id.textFilesToDeleteValue);
        TextView spaceValueText = findViewById(R.id.textSpaceToFreeValue);
        filesValueText.setText(getString(R.string.photos_count, selectedItems.size()));
        spaceValueText.setText(FormatUtils.formatStorage(ScanSessionStore.getInstance().getSelectedBytes()));

        deleteButton = findViewById(R.id.buttonDelete);
        deleteButton.setOnClickListener(v -> beginDeleteFlow());
        findViewById(R.id.buttonCancel).setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void beginDeleteFlow() {
        setDeleteButtonEnabled(false);
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
        pendingLegacyItems = new ArrayList<>(items);
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
                    pendingLegacyItems = items;
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
        if (outcome.getDeletedCount() == 0) {
            Toast.makeText(this, R.string.delete_failed_message, Toast.LENGTH_LONG).show();
            setDeleteButtonEnabled(true);
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

    private void setDeleteButtonEnabled(boolean enabled) {
        deleteButton.setEnabled(enabled);
        deleteButton.setAlpha(enabled ? 1f : 0.7f);
    }
}
