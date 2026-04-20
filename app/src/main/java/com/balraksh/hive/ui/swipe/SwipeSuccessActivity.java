package com.balraksh.hive.ui.swipe;

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
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.BottomNavController;
import com.balraksh.hive.ui.cleanup.CleanupCelebrationView;
import com.balraksh.hive.ui.home.HomeActivity;
import com.balraksh.hive.utils.FormatUtils;

import java.text.DecimalFormat;

public class SwipeSuccessActivity extends BaseEdgeToEdgeActivity {

    public static final String EXTRA_FREED_BYTES = "extra_freed_bytes";
    public static final String EXTRA_DELETED_COUNT = "extra_deleted_count";
    public static final String EXTRA_REVIEWED_COUNT = "extra_reviewed_count";

    private TextView freedValueText;
    private TextView freedUnitText;
    private TextView filesCountText;
    private TextView reviewedCountText;
    private TextView storagePercentText;

    private long freedBytes;
    private int deletedCount;
    private int reviewedCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_swipe_success);

        freedBytes = Math.max(0L, getIntent().getLongExtra(EXTRA_FREED_BYTES, 0L));
        deletedCount = Math.max(0, getIntent().getIntExtra(EXTRA_DELETED_COUNT, 0));
        reviewedCount = Math.max(0, getIntent().getIntExtra(EXTRA_REVIEWED_COUNT, 0));

        BottomNavController.bind(this, BottomNavController.TAB_SWIPE);
        bindViews();
        bindOutcome();
        animateEntrance();

        findViewById(R.id.buttonBackHome).setOnClickListener(v -> openHome());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                openHome();
            }
        });
    }

    private void bindViews() {
        CleanupCelebrationView celebrationView = findViewById(R.id.viewCelebration);
        celebrationView.setPalette(
                getColor(R.color.color_scan_gold),
                getColor(R.color.color_cleanup_success_accent),
                getColor(R.color.white)
        );

        freedValueText = findViewById(R.id.textFreedValue);
        freedUnitText = findViewById(R.id.textFreedUnit);
        filesCountText = findViewById(R.id.textFilesCount);
        reviewedCountText = findViewById(R.id.textReviewedCount);
        storagePercentText = findViewById(R.id.textStoragePercent);
    }

    private void bindOutcome() {
        updateFreedAmount(0L);
        filesCountText.setText("0");
        reviewedCountText.setText("0");
        storagePercentText.setText("+0%");

        animateFreedAmount(freedBytes);
        animateInt(filesCountText, deletedCount, false, 0L);
        animateInt(reviewedCountText, reviewedCount, false, 60L);
        animateInt(storagePercentText, calculateRecoveredPercent(freedBytes), true, 120L);
    }

    private void animateEntrance() {
        animateUp(findViewById(R.id.layoutHeroIcon), 0L, 28f);
        animateUp(findViewById(R.id.textSuccessTitle), 90L, 24f);
        animateUp(findViewById(R.id.textSuccessSubtitle), 150L, 20f);
        animateUp(findViewById(R.id.cardSummary), 220L, 36f);
        animateUp(findViewById(R.id.buttonBackHome), 320L, 24f);

        View heroTile = findViewById(R.id.viewHeroTile);
        heroTile.setScaleX(0.82f);
        heroTile.setScaleY(0.82f);
        heroTile.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(520L)
                .setInterpolator(new OvershootInterpolator(1.8f))
                .start();

        ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(
                findViewById(R.id.viewHeroGlow),
                View.ALPHA,
                0.5f,
                1f,
                0.5f
        );
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

    private void animateInt(TextView textView, int targetValue, boolean withPrefix, long extraDelay) {
        ValueAnimator animator = ValueAnimator.ofInt(0, targetValue);
        animator.setDuration(900L);
        animator.setStartDelay(280L + extraDelay);
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
        animator.addUpdateListener(animation ->
                updateFreedAmount((long) (bytes * (float) animation.getAnimatedValue())));
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

    private int calculateRecoveredPercent(long bytes) {
        try {
            StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            long totalBytes = statFs.getTotalBytes();
            if (totalBytes <= 0L || bytes <= 0L) {
                return 0;
            }
            return Math.max(1, (int) Math.round((bytes * 100d) / totalBytes));
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
