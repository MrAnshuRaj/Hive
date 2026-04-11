package com.balraksh.safkaro.ui.scanning;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.data.ScanConfig;
import com.balraksh.safkaro.data.ScanProgress;
import com.balraksh.safkaro.data.ScanResult;
import com.balraksh.safkaro.repository.CleanupPreferences;
import com.balraksh.safkaro.repository.MediaRepository;
import com.balraksh.safkaro.repository.ScanSessionStore;
import com.balraksh.safkaro.ui.BaseEdgeToEdgeActivity;
import com.balraksh.safkaro.ui.results.ScanResultsActivity;

public class ScanningActivity extends BaseEdgeToEdgeActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ProgressBar progressBar;
    private TextView progressPercentText;
    private TextView stageText;
    private TextView filesScannedValueText;
    private TextView matchesFoundValueText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_scanning);

        progressBar = findViewById(R.id.progressBar);
        progressPercentText = findViewById(R.id.textProgressPercent);
        stageText = findViewById(R.id.textStage);
        filesScannedValueText = findViewById(R.id.textFilesScannedValue);
        matchesFoundValueText = findViewById(R.id.textMatchesFoundValue);

        ScanConfig config = ScanConfig.fromIntent(getIntent());
        runScan(config);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @Override
    public void onBackPressed() {
    }

    private void runScan(ScanConfig config) {
        executor.execute(() -> {
            try {
                MediaRepository repository = new MediaRepository(this);
                ScanResult result = repository.scanImages(config, this::renderProgress);
                runOnUiThread(() -> {
                    ScanSessionStore.getInstance().setCurrentResult(config, result);
                    new CleanupPreferences(this).setLastScanPotentialBytes(result.getPotentialSpaceBytes());
                    startActivity(new Intent(this, ScanResultsActivity.class));
                    finish();
                });
            } catch (IOException exception) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.scan_error_message, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void renderProgress(ScanProgress progress) {
        runOnUiThread(() -> {
            progressBar.setProgress(progress.getProgressPercent());
            progressPercentText.setText(progress.getProgressPercent() + "%");
            stageText.setText(progress.getStageLabel());
            filesScannedValueText.setText(String.valueOf(progress.getScannedCount()));
            matchesFoundValueText.setText(String.valueOf(progress.getMatchCount()));
        });
    }
}
