package com.balraksh.hive.ui.cleanup;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;

import com.balraksh.hive.R;
import com.balraksh.hive.data.CleanupOutcome;
import com.balraksh.hive.repository.ScanSessionStore;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.home.HomeActivity;
import com.balraksh.hive.utils.FormatUtils;

import java.text.DecimalFormat;

public class CleanupSuccessActivity extends BaseEdgeToEdgeActivity {

    private TextView freedValueText;
    private TextView freedUnitText;
    private TextView filesCountText;
    private TextView photosCountText;
    private TextView storagePercentText;
    private TextView noteText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_cleanup_success);

        CleanupOutcome outcome = ScanSessionStore.getInstance().getLastOutcome();
        if (outcome == null) {
            finish();
            return;
        }

        freedValueText = findViewById(R.id.textFreedValue);
        freedUnitText = findViewById(R.id.textFreedUnit);
        filesCountText = findViewById(R.id.textFilesCount);
        photosCountText = findViewById(R.id.textPhotosCount);
        storagePercentText = findViewById(R.id.textStoragePercent);
        noteText = findViewById(R.id.textCleanupNote);

        bindOutcome(outcome);
        animateEntrance();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                openHome();
            }
        });

        findViewById(R.id.buttonBackHome).setOnClickListener(v -> openHome());
    }

    private void bindOutcome(CleanupOutcome outcome) {
        updateFreedAmount(outcome.getFreedBytes());
        filesCountText.setText("0");
        photosCountText.setText("0");
        storagePercentText.setText("+0%");

        int deletedCount = outcome.getDeletedCount();
        int percentRecovered = calculateRecoveredPercent(outcome.getFreedBytes());

        animateInt(filesCountText, deletedCount, false);
        animateInt(photosCountText, deletedCount, false);
        animateInt(storagePercentText, percentRecovered, true);
        animateFreedAmount(outcome.getFreedBytes());

        if (outcome.getFailedCount() > 0) {
            noteText.setVisibility(View.VISIBLE);
            noteText.setText(getString(R.string.partial_delete_message, outcome.getFailedCount()));
        } else {
            noteText.setVisibility(View.GONE);
        }
    }

    private void animateEntrance() {
        animateUp(findViewById(R.id.layoutHeroIcon), 0L, 28f);
        animateUp(findViewById(R.id.textSuccessTitle), 90L, 24f);
        animateUp(findViewById(R.id.textSuccessSubtitle), 150L, 20f);
        animateUp(findViewById(R.id.cardSummary), 220L, 36f);
        animateUp(findViewById(R.id.textCleanupNote), 280L, 24f);
        animateUp(findViewById(R.id.buttonBackHome), 330L, 28f);

        View heroTile = findViewById(R.id.viewHeroTile);
        heroTile.setScaleX(0.82f);
        heroTile.setScaleY(0.82f);
        heroTile.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(520L)
                .setInterpolator(new OvershootInterpolator(1.8f))
                .start();

        ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(findViewById(R.id.viewHeroGlow), View.ALPHA, 0.55f, 1f, 0.55f);
        pulseAnimator.setDuration(2200L);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.start();
    }

    private void animateUp(View view, long delay, float distance) {
        view.setAlpha(0f);
        view.setTranslationY(distance);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(340L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateInt(TextView textView, int targetValue, boolean withPrefix) {
        ValueAnimator animator = ValueAnimator.ofInt(0, targetValue);
        animator.setDuration(900L);
        animator.setStartDelay(280L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            textView.setText(withPrefix ? "+" + value + "%" : String.valueOf(value));
        });
        animator.start();
    }

    private void animateFreedAmount(long bytes) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1050L);
        animator.setStartDelay(240L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> updateFreedAmount((long) (bytes * (float) animation.getAnimatedValue())));
        animator.start();
    }

    private void updateFreedAmount(long bytes) {
        String formatted = FormatUtils.formatStorage(bytes);
        int separator = formatted.lastIndexOf(' ');
        if (separator > 0) {
            freedValueText.setText(formatted.substring(0, separator));
            freedUnitText.setText(formatted.substring(separator + 1));
        } else {
            freedValueText.setText(new DecimalFormat("0.0").format(bytes / (1024f * 1024f)));
            freedUnitText.setText("MB");
        }
    }

    private int calculateRecoveredPercent(long freedBytes) {
        try {
            StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            long totalBytes = statFs.getTotalBytes();
            if (totalBytes <= 0L || freedBytes <= 0L) {
                return 0;
            }
            int percent = (int) Math.round((freedBytes * 100d) / totalBytes);
            return Math.max(1, percent);
        } catch (Exception exception) {
            return 0;
        }
    }

    private void openHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
