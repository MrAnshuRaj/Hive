package com.balraksh.hive.ui.video;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import com.balraksh.hive.R;
import com.balraksh.hive.adapters.VideoListAdapter;
import com.balraksh.hive.data.VideoItem;
import com.balraksh.hive.repository.VideoCompressionSessionStore;
import com.balraksh.hive.repository.VideoMediaRepository;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.utils.PermissionHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoSelectActivity extends BaseEdgeToEdgeActivity {

    private static final String STATE_SELECTED_URIS = "selected_uris";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<VideoItem> pendingDeleteItems = new ArrayList<>();

    private VideoMediaRepository repository;
    private VideoCompressionSessionStore sessionStore;
    private VideoListAdapter adapter;
    private MaterialButton continueButton;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private MaterialButton emptyActionButton;

    private final List<VideoItem> loadedVideos = new ArrayList<>();
    private final Set<String> selectedUris = new LinkedHashSet<>();

    private final ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult(), result -> {
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
            public void onVideoLongPressed(@NonNull VideoItem item, @NonNull View anchor) {
                showVideoActions(item, anchor);
            }

            @Override
            public void onDeleteClicked(@NonNull VideoItem item) {
                confirmDelete(item);
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerVideos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        continueButton = findViewById(R.id.buttonContinue);
        emptyState = findViewById(R.id.layoutEmptyState);
        emptyTitle = findViewById(R.id.textEmptyTitle);
        emptyMessage = findViewById(R.id.textEmptyMessage);
        emptyActionButton = findViewById(R.id.buttonEmptyAction);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        continueButton.setOnClickListener(v -> continueToSetup());

        if (savedInstanceState != null) {
            ArrayList<String> savedSelection = savedInstanceState.getStringArrayList(STATE_SELECTED_URIS);
            if (savedSelection != null) {
                selectedUris.addAll(savedSelection);
            }
        } else {
            for (VideoItem item : sessionStore.getSelectedVideos()) {
                selectedUris.add(item.getUriString());
            }
        }

        if (!PermissionHelper.hasRequiredPermissions(this)) {
            showPermissionState();
            return;
        }
        loadVideos();
        updateContinueButton();
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
    }

    private void loadVideos() {
        emptyState.setVisibility(View.GONE);
        executor.execute(() -> {
            List<VideoItem> videos = repository.loadDeviceVideos();
            runOnUiThread(() -> {
                loadedVideos.clear();
                loadedVideos.addAll(videos);
                adapter.submitList(new ArrayList<>(videos));
                adapter.setSelectedUris(selectedUris);
                if (videos.isEmpty()) {
                    showNoVideosState();
                }
            });
        });
    }

    private void toggleSelection(@NonNull VideoItem item) {
        if (selectedUris.contains(item.getUriString())) {
            selectedUris.remove(item.getUriString());
        } else {
            selectedUris.add(item.getUriString());
        }
        adapter.setSelectedUris(selectedUris);
        updateContinueButton();
    }

    private void updateContinueButton() {
        int count = selectedUris.size();
        continueButton.setEnabled(count > 0);
        continueButton.setText(count > 0
                ? getString(R.string.continue_with_count, count)
                : getString(R.string.continue_disabled));
    }

    private void continueToSetup() {
        if (selectedUris.isEmpty()) {
            Toast.makeText(this, R.string.select_at_least_one_video, Toast.LENGTH_SHORT).show();
            return;
        }
        List<VideoItem> selectedVideos = new ArrayList<>();
        for (VideoItem item : loadedVideos) {
            if (selectedUris.contains(item.getUriString())) {
                selectedVideos.add(item);
            }
        }
        sessionStore.setSelectedVideos(selectedVideos);
        startActivity(new Intent(this, VideoSetupActivity.class));
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
                        java.util.Collections.singletonList(item.getUri())
                );
                deleteRequestLauncher.launch(new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build());
            } catch (Exception exception) {
                pendingDeleteItems.clear();
                Toast.makeText(this, R.string.delete_video_failed, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        executor.execute(() -> {
            boolean deleted = false;
            try {
                deleted = getContentResolver().delete(item.getUri(), null, null) > 0;
            } catch (SecurityException exception) {
                deleted = false;
            }
            final boolean wasDeleted = deleted;
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
        updateContinueButton();
    }

    private void removeDeletedItem(@NonNull VideoItem item) {
        selectedUris.remove(item.getUriString());
        loadedVideos.remove(item);
        adapter.submitList(new ArrayList<>(loadedVideos));
        adapter.setSelectedUris(selectedUris);
        updateContinueButton();
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
}

