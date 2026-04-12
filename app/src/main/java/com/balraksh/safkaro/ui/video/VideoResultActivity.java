package com.balraksh.safkaro.ui.video;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.repository.VideoCompressionSessionStore;
import com.balraksh.safkaro.ui.BaseEdgeToEdgeActivity;
import com.balraksh.safkaro.utils.FormatUtils;
import com.balraksh.safkaro.video.VideoCompressionResult;

import java.util.ArrayList;
import java.util.List;

public class VideoResultActivity extends BaseEdgeToEdgeActivity {

    private VideoCompressionSessionStore sessionStore;
    private List<VideoCompressionResult> results;
    private List<VideoCompressionResult> successfulResults;
    private VideoCompressionResult primaryResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_video_result);

        sessionStore = VideoCompressionSessionStore.getInstance(this);
        results = sessionStore.getResults();
        successfulResults = getSuccessfulResults(results);
        primaryResult = !successfulResults.isEmpty() ? successfulResults.get(0) : (results.isEmpty() ? null : results.get(0));
        if (primaryResult == null) {
            finish();
            return;
        }

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonPreviewPlay).setOnClickListener(v -> playPrimary());
        findViewById(R.id.buttonPlay).setOnClickListener(v -> playPrimary());
        findViewById(R.id.buttonShare).setOnClickListener(v -> shareOutputs());
        findViewById(R.id.buttonSave).setOnClickListener(v ->
                Toast.makeText(this, R.string.video_saved_location, Toast.LENGTH_SHORT).show());
        findViewById(R.id.buttonCompressMore).setOnClickListener(v -> openSelectAgain());

        bindPreview();
        bindSummary();
        bindActions();
    }

    private void bindPreview() {
        ImageView previewView = findViewById(R.id.imagePreview);
        Uri previewUri = primaryResult.isSuccess() ? primaryResult.getOutputUri() : primaryResult.getSourceUri();
        Glide.with(previewView)
                .load(previewUri)
                .centerCrop()
                .into(previewView);
    }

    private void bindSummary() {
        long totalOriginal = 0L;
        long totalCompressed = 0L;
        for (VideoCompressionResult result : results) {
            totalOriginal += result.getInputSizeBytes();
            if (result.isSuccess()) {
                totalCompressed += result.getOutputSizeBytes();
            }
        }
        long savedBytes = Math.max(0L, totalOriginal - totalCompressed);
        int savedPercent = totalOriginal > 0L ? (int) ((savedBytes * 100L) / totalOriginal) : 0;

        ((TextView) findViewById(R.id.textOriginalSize)).setText(FormatUtils.formatStorage(totalOriginal));
        ((TextView) findViewById(R.id.textCompressedSize)).setText(FormatUtils.formatStorage(totalCompressed));
        ((TextView) findViewById(R.id.textSpaceSaved)).setText(getString(R.string.space_saved_percent, savedPercent));

        TextView headlineView = findViewById(R.id.textResultHeadline);
        TextView subheadlineView = findViewById(R.id.textResultSubheadline);
        if (successfulResults.isEmpty()) {
            headlineView.setText(R.string.video_all_failed);
            subheadlineView.setText(primaryResult.getErrorMessage());
        } else if (successfulResults.size() < results.size()) {
            headlineView.setText(getString(R.string.video_partial_success, successfulResults.size(), results.size()));
            subheadlineView.setText(R.string.video_saved_location);
        } else {
            headlineView.setText(primaryResult.getOutputDisplayName());
            subheadlineView.setText(R.string.video_saved_location);
        }
    }

    private void bindActions() {
        boolean hasSuccess = !successfulResults.isEmpty();
        findViewById(R.id.buttonPreviewPlay).setEnabled(hasSuccess);
        findViewById(R.id.buttonPlay).setEnabled(hasSuccess);
        findViewById(R.id.buttonShare).setEnabled(hasSuccess);
        findViewById(R.id.buttonSave).setEnabled(hasSuccess);
    }

    private void playPrimary() {
        if (successfulResults.isEmpty()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(primaryResult.getOutputUri(), "video/mp4");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.play)));
    }

    private void shareOutputs() {
        if (successfulResults.isEmpty()) {
            return;
        }
        if (successfulResults.size() == 1) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/mp4");
            intent.putExtra(Intent.EXTRA_STREAM, successfulResults.get(0).getOutputUri());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.share)));
            return;
        }
        ArrayList<Uri> uris = new ArrayList<>();
        for (VideoCompressionResult result : successfulResults) {
            uris.add(result.getOutputUri());
        }
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("video/mp4");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void openSelectAgain() {
        sessionStore.resetSelection();
        Intent intent = new Intent(this, VideoSelectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private List<VideoCompressionResult> getSuccessfulResults(List<VideoCompressionResult> allResults) {
        List<VideoCompressionResult> success = new ArrayList<>();
        for (VideoCompressionResult result : allResults) {
            if (result.isSuccess()) {
                success.add(result);
            }
        }
        return success;
    }
}
