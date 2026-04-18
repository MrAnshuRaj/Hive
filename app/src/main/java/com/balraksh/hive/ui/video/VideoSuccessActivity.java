package com.balraksh.hive.ui.video;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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

public class VideoSuccessActivity extends BaseEdgeToEdgeActivity {

    public static final String EXTRA_FREED_BYTES = "extra_freed_bytes";
    public static final String EXTRA_VIDEO_COUNT = "extra_video_count";
    public static final String EXTRA_TOTAL_DURATION_MS = "extra_total_duration_ms";

    private TextView freedValueText;
    private TextView freedUnitText;
    private TextView filesCountText;
    private TextView minutesCountText;
    private TextView storagePercentText;

    private long freedBytes;
    private int videoCount;
    private long totalDurationMs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_video_success);

        freedBytes = getIntent().getLongExtra(EXTRA_FREED_BYTES, 0L);
        if (freedBytes < 0L) {
            freedBytes = 0L;
        }
        videoCount = Math.max(0, getIntent().getIntExtra(EXTRA_VIDEO_COUNT, 0));
        totalDurationMs = Math.max(0L, getIntent().getLongExtra(EXTRA_TOTAL_DURATION_MS, 0L));

        BottomNavController.bind(this, BottomNavController.TAB_COMPRESS);
        bindViews();
        bindOutcome();
        animateEntrance();
        bindActions();

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
                getColor(R.color.color_primary),
                getColor(R.color.color_primary_soft),
                getColor(R.color.white)
        );

        freedValueText = findViewById(R.id.textFreedValue);
        freedUnitText = findViewById(R.id.textFreedUnit);
        filesCountText = findViewById(R.id.textFilesCount);
        minutesCountText = findViewById(R.id.textMinutesCount);
        storagePercentText = findViewById(R.id.textStoragePercent);
    }

    private void bindOutcome() {
        updateFreedAmount(0L);
        filesCountText.setText("0");
        minutesCountText.setText("0");
        storagePercentText.setText("+0%");

        int filesCount = Math.max(1, videoCount);
        int minutesCount = Math.max(1, (int) Math.ceil(totalDurationMs / 60000d));
        int storagePercent = calculateRecoveredPercent(freedBytes);

        animateFreedAmount(freedBytes);
        animateInt(filesCountText, filesCount, false, 0L);
        animateInt(minutesCountText, minutesCount, false, 60L);
        animateInt(storagePercentText, storagePercent, true, 120L);
    }

    private void bindActions() {
        findViewById(R.id.buttonBackHome).setOnClickListener(v -> openHome());
        findViewById(R.id.buttonShareResults).setOnClickListener(v -> shareResults());
        findViewById(R.id.buttonShareApp).setOnClickListener(v -> shareApp());
        findViewById(R.id.buttonRateApp).setOnClickListener(v -> rateApp());
    }

    private void animateEntrance() {
        animateUp(findViewById(R.id.layoutHeroIcon), 0L, 28f);
        animateUp(findViewById(R.id.textSuccessTitle), 90L, 24f);
        animateUp(findViewById(R.id.textSuccessSubtitle), 150L, 20f);
        animateUp(findViewById(R.id.cardSummary), 220L, 36f);
        animateUp(findViewById(R.id.layoutStats), 280L, 24f);
        animateUp(findViewById(R.id.layoutPrimaryAction), 320L, 24f);
        animateUp(findViewById(R.id.layoutSecondaryActions), 380L, 24f);
        animateUp(findViewById(R.id.buttonRateApp), 440L, 24f);

        View heroTile = findViewById(R.id.viewHeroTile);
        heroTile.setScaleX(0.82f);
        heroTile.setScaleY(0.82f);
        heroTile.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(520L)
                .setInterpolator(new OvershootInterpolator(1.8f))
                .start();
        startBreathingAnimation(heroTile, 1.0f, 1.045f, 2800L);
        startBreathingAnimation(findViewById(R.id.cardSummary), 1.0f, 1.012f, 3400L);

        ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(findViewById(R.id.viewHeroGlow), View.ALPHA, 0.5f, 1f, 0.5f);
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
        animator.addUpdateListener(animation -> updateFreedAmount((long) (bytes * (float) animation.getAnimatedValue())));
        animator.start();
    }

    private void startBreathingAnimation(View view, float startScale, float endScale, long durationMs) {
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(view, View.SCALE_X, startScale, endScale);
        scaleXAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        scaleXAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        scaleXAnimator.setDuration(durationMs);

        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(view, View.SCALE_Y, startScale, endScale);
        scaleYAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        scaleYAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        scaleYAnimator.setDuration(durationMs);

        scaleXAnimator.start();
        scaleYAnimator.start();
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

    private void shareResults() {
        String message = getString(
                R.string.video_share_results_message,
                FormatUtils.formatStorage(freedBytes)
        );
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.video_share_results)));
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(
                R.string.home_share_message,
                "https://play.google.com/store/apps/details?id=" + getPackageName()
        ));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.home_share_with)));
    }

    private void rateApp() {
        Uri marketUri = Uri.parse("market://details?id=" + getPackageName());
        Intent intent = new Intent(Intent.ACTION_VIEW, marketUri);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())
            ));
        }
    }

    private void openHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
