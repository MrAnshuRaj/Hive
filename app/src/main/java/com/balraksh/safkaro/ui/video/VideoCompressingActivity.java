package com.balraksh.safkaro.ui.video;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.repository.VideoCompressionSessionStore;
import com.balraksh.safkaro.ui.BaseEdgeToEdgeActivity;
import com.balraksh.safkaro.video.VideoCompressionProgress;
import com.balraksh.safkaro.video.VideoCompressionStage;

public class VideoCompressingActivity extends BaseEdgeToEdgeActivity implements VideoCompressionSessionStore.Listener {

    private VideoCompressionSessionStore sessionStore;
    private TextView currentFileView;
    private TextView currentPositionView;
    private TextView stageView;
    private TextView progressValueView;
    private TextView estimatedTimeView;
    private LinearProgressIndicator progressIndicator;
    private boolean navigatedToResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_video_compressing);

        sessionStore = VideoCompressionSessionStore.getInstance(this);
        currentFileView = findViewById(R.id.textCurrentFile);
        currentPositionView = findViewById(R.id.textCurrentPosition);
        stageView = findViewById(R.id.textStage);
        progressValueView = findViewById(R.id.textProgressValue);
        estimatedTimeView = findViewById(R.id.textEstimatedTime);
        progressIndicator = findViewById(R.id.progressLinear);

        findViewById(R.id.buttonBack).setOnClickListener(v -> handleBackPressed());
        findViewById(R.id.buttonRunInBackground).setOnClickListener(v -> moveTaskToBack(true));
    }

    @Override
    protected void onStart() {
        super.onStart();
        sessionStore.addListener(this);
        bindState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sessionStore.removeListener(this);
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }

    @Override
    public void onSessionUpdated() {
        runOnUiThread(this::bindState);
    }

    private void bindState() {
        VideoCompressionSessionStore.State state = sessionStore.getState();
        if (state == VideoCompressionSessionStore.State.COMPLETED && !navigatedToResult) {
            navigatedToResult = true;
            startActivity(new Intent(this, VideoResultActivity.class));
            finish();
            return;
        }
        if (state == VideoCompressionSessionStore.State.CANCELLED) {
            Toast.makeText(this, R.string.compression_cancelled, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        VideoCompressionProgress progress = sessionStore.getLatestProgress();
        if (progress == null) {
            currentFileView.setText("");
            currentPositionView.setText("");
            stageView.setText(R.string.compression_stage_preparing);
            progressValueView.setText("0%");
            progressIndicator.setProgress(0);
            estimatedTimeView.setText(R.string.less_than_a_minute);
            return;
        }

        currentFileView.setText(progress.getCurrentFileName());
        currentPositionView.setText(getString(
                R.string.video_position_count,
                progress.getCurrentIndex(),
                progress.getTotalCount()
        ));
        stageView.setText(getStageLabel(progress.getStage()));
        progressValueView.setText(progress.getOverallProgressPercent() + "%");
        progressIndicator.setProgress(progress.getOverallProgressPercent());
        estimatedTimeView.setText(formatRemaining(progress.getEstimatedRemainingMs()));
    }

    private String formatRemaining(long remainingMs) {
        if (remainingMs <= 60_000L) {
            return getString(R.string.less_than_a_minute);
        }
        int minutes = (int) Math.ceil(remainingMs / 60_000d);
        return getString(R.string.remaining_minutes, minutes);
    }

    private int getStageLabel(VideoCompressionStage stage) {
        switch (stage) {
            case EXTRACTING:
                return R.string.compression_stage_extracting;
            case ENCODING:
                return R.string.compression_stage_encoding;
            case MUXING:
                return R.string.compression_stage_muxing;
            case SAVING:
                return R.string.compression_stage_saving;
            case COMPLETED:
                return R.string.compression_stage_complete;
            case PREPARING:
            default:
                return R.string.compression_stage_preparing;
        }
    }

    private void handleBackPressed() {
        if (!sessionStore.isRunning()) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.compression_cancel_title)
                .setMessage(R.string.compression_cancel_message)
                .setPositiveButton(R.string.stop_compression, (dialog, which) -> sessionStore.cancelCompression())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
