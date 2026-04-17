package com.balraksh.hive.ui.setup.folder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.balraksh.hive.R;
import com.balraksh.hive.adapters.FolderOptionAdapter;
import com.balraksh.hive.data.BucketOption;
import com.balraksh.hive.data.ScanConfig;
import com.balraksh.hive.data.ScanMode;
import com.balraksh.hive.repository.MediaRepository;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.scanning.ScanningActivity;

public class FolderSelectActivity extends BaseEdgeToEdgeActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private FolderOptionAdapter adapter;
    private MaterialButton continueButton;
    private View emptyState;
    private final List<BucketOption> selectedBuckets = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_folder_select);

        adapter = new FolderOptionAdapter(buckets -> {
            selectedBuckets.clear();
            selectedBuckets.addAll(buckets);
            updateContinueButton();
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerFolders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        continueButton = findViewById(R.id.buttonContinue);
        emptyState = findViewById(R.id.layoutEmptyState);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        continueButton.setOnClickListener(v -> startSelectedScan());

        updateContinueButton();
        loadFolders();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void loadFolders() {
        executor.execute(() -> {
            List<BucketOption> options = new MediaRepository(this).loadBuckets();
            runOnUiThread(() -> renderFolders(options));
        });
    }

    private void renderFolders(List<BucketOption> options) {
        boolean hasFolders = options != null && !options.isEmpty();
        emptyState.setVisibility(hasFolders ? View.GONE : View.VISIBLE);
        findViewById(R.id.recyclerFolders).setVisibility(hasFolders ? View.VISIBLE : View.GONE);
        if (!hasFolders) {
            continueButton.setEnabled(false);
            return;
        }
        adapter.submitList(options);
    }

    private void updateContinueButton() {
        int selectedCount = selectedBuckets.size();
        boolean hasSelection = selectedCount > 0;
        continueButton.setEnabled(hasSelection);
        continueButton.setText(hasSelection
                ? getResources().getQuantityString(R.plurals.scan_selected_folders, selectedCount, selectedCount)
                : getString(R.string.select_folder_to_scan_disabled));
    }

    private void startSelectedScan() {
        if (selectedBuckets.isEmpty()) {
            Toast.makeText(this, R.string.select_a_folder_first, Toast.LENGTH_SHORT).show();
            return;
        }

        long[] selectedBucketIds = new long[selectedBuckets.size()];
        String[] selectedBucketNames = new String[selectedBuckets.size()];
        for (int index = 0; index < selectedBuckets.size(); index++) {
            BucketOption bucket = selectedBuckets.get(index);
            selectedBucketIds[index] = bucket.getBucketId();
            selectedBucketNames[index] = bucket.getBucketName();
        }

        ScanConfig config = new ScanConfig(
                ScanMode.SELECTED_BUCKET,
                true,
                true,
                selectedBucketIds[0],
                selectedBucketNames[0],
                selectedBucketIds,
                selectedBucketNames
        );
        Intent intent = new Intent(this, ScanningActivity.class);
        config.writeToIntent(intent);
        startActivity(intent);
        finish();
    }
}
