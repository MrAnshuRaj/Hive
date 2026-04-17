package com.balraksh.hive.ui.scanning;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.balraksh.hive.R;
import com.balraksh.hive.data.ScanConfig;
import com.balraksh.hive.data.ScanProgress;
import com.balraksh.hive.data.ScanResult;
import com.balraksh.hive.repository.CleanupPreferences;
import com.balraksh.hive.repository.MediaRepository;
import com.balraksh.hive.repository.ScanSessionStore;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.results.ScanResultsActivity;

public class ScanningActivity extends BaseEdgeToEdgeActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final NumberFormat wholeNumberFormat = NumberFormat.getIntegerInstance();

    private TextView progressPercentText;
    private TextView stageText;
    private TextView filesScannedValueText;
    private TextView matchesFoundValueText;
    private ObjectAnimator stageAnimator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_scanning);

        progressPercentText = findViewById(R.id.textProgressPercent);
        stageText = findViewById(R.id.textStage);
        filesScannedValueText = findViewById(R.id.textFilesScannedValue);
        matchesFoundValueText = findViewById(R.id.textMatchesFoundValue);

        playEntranceAnimations();
        startStagePulseAnimation();

        ScanConfig config = ScanConfig.fromIntent(getIntent());
        runScan(config);
    }

    @Override
    protected void onDestroy() {
        if (stageAnimator != null) {
            stageAnimator.cancel();
        }
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
            progressPercentText.setText(progress.getProgressPercent() + "%");
            stageText.setText(progress.getStageLabel());
            filesScannedValueText.setText(wholeNumberFormat.format(progress.getScannedCount()));
            matchesFoundValueText.setText(wholeNumberFormat.format(
                    progress.getDuplicateMatchCount() + progress.getSimilarMatchCount()
            ));
            pulseProgressText();
        });
    }

    private void playEntranceAnimations() {
        View headerTitle = findViewById(R.id.textHeaderTitle);
        View headerSubtitle = findViewById(R.id.textHeaderSubtitle);
        View loader = findViewById(R.id.layoutLoader);
        View heroTitle = findViewById(R.id.textHeroTitle);
        View statsCard = findViewById(R.id.cardStats);

        prepareForEntrance(headerTitle, 16f);
        prepareForEntrance(headerSubtitle, 18f);
        prepareForEntrance(loader, 24f);
        prepareForEntrance(heroTitle, 20f);
        prepareForEntrance(statsCard, 28f);

        animateEntrance(headerTitle, 0L);
        animateEntrance(headerSubtitle, 120L);
        animateEntrance(loader, 220L);
        animateEntrance(heroTitle, 320L);
        animateEntrance(statsCard, 420L);
    }

    private void prepareForEntrance(View view, float translationY) {
        view.setAlpha(0f);
        view.setTranslationY(dp(translationY));
    }

    private void animateEntrance(View view, long delay) {
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(500L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void startStagePulseAnimation() {
        stageAnimator = ObjectAnimator.ofFloat(stageText, View.ALPHA, 0f, 1f, 1f, 0f);
        stageAnimator.setDuration(2000L);
        stageAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        stageAnimator.setRepeatMode(ObjectAnimator.RESTART);
        stageAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        stageAnimator.start();
    }

    private void pulseProgressText() {
        progressPercentText.animate().cancel();
        progressPercentText.setScaleX(1f);
        progressPercentText.setScaleY(1f);
        progressPercentText.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(140L)
                .withEndAction(() -> progressPercentText.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(160L)
                        .start())
                .start();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}

