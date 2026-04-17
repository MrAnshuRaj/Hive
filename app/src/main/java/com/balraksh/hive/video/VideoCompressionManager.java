package com.balraksh.hive.video;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.Nullable;
import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.Presentation;
import androidx.media3.transformer.AudioEncoderSettings;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.DefaultEncoderFactory;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.EditedMediaItemSequence;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.ProgressHolder;
import androidx.media3.transformer.Transformer;
import androidx.media3.transformer.VideoEncoderSettings;

import com.balraksh.hive.data.VideoItem;
import com.balraksh.hive.repository.VideoMediaRepository;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UnstableApi
public class VideoCompressionManager {

    private static final float I_FRAME_INTERVAL_SECONDS = 2f;

    public interface Callback {
        void onProgress(@NonNull VideoCompressionProgress progress);

        void onItemResult(@NonNull VideoCompressionResult result);

        void onBatchFinished(@NonNull List<VideoCompressionResult> results, boolean cancelled);
    }

    private final Context appContext;
    private final VideoMediaRepository repository;
    private final Handler mainHandler;
    private final ExecutorService ioExecutor;
    private final List<VideoCompressionResult> results = new ArrayList<>();

    private Transformer currentTransformer;
    private Runnable progressRunnable;
    private Callback callback;
    private List<VideoItem> queue = new ArrayList<>();
    private VideoCompressionRequest request;
    private boolean cancelled;
    private boolean running;
    private int currentIndex;
    private long batchStartedAtMs;

    public VideoCompressionManager(@NonNull Context context) {
        appContext = context.getApplicationContext();
        repository = new VideoMediaRepository(appContext);
        mainHandler = new Handler(Looper.getMainLooper());
        ioExecutor = Executors.newSingleThreadExecutor();
    }

    @MainThread
    public void startCompression(
            @NonNull List<VideoItem> selectedVideos,
            @NonNull VideoCompressionRequest compressionRequest,
            @NonNull Callback compressionCallback
    ) {
        cancelInternal(false);
        queue = new ArrayList<>(selectedVideos);
        request = compressionRequest;
        callback = compressionCallback;
        results.clear();
        cancelled = false;
        running = true;
        currentIndex = 0;
        batchStartedAtMs = SystemClock.elapsedRealtime();

        if (queue.isEmpty()) {
            running = false;
            callback.onBatchFinished(new ArrayList<>(results), false);
            return;
        }
        dispatchProgress(queue.get(0), 0, VideoCompressionStage.PREPARING);
        startNextItem();
    }

    @MainThread
    public void cancel() {
        cancelInternal(true);
    }

    @OptIn(markerClass = UnstableApi.class)
    @MainThread
    private void cancelInternal(boolean userInitiated) {
        cancelled = userInitiated;
        stopProgressPolling();
        if (currentTransformer != null) {
            currentTransformer.cancel();
            currentTransformer = null;
        } else if (running && callback != null) {
            running = false;
            callback.onBatchFinished(new ArrayList<>(results), cancelled);
        }
    }

    @MainThread
    private void startNextItem() {
        if (cancelled) {
            finishBatch(true);
            return;
        }
        if (currentIndex >= queue.size()) {
            finishBatch(false);
            return;
        }

        VideoItem item = queue.get(currentIndex);
        dispatchProgress(item, 0, VideoCompressionStage.PREPARING);
        ioExecutor.execute(() -> {
            try {
                VideoMetadata metadata = repository.loadMetadata(item);
                ResolvedVideoCompressionSettings settings =
                        VideoCompressionSettingsResolver.resolve(metadata, request);
                mainHandler.post(() -> exportItem(item, metadata, settings));
            } catch (IOException exception) {
                handleItemFailure(item, exception);
            }
        });
    }

    @OptIn(markerClass = UnstableApi.class)
    @MainThread
    private void exportItem(
            @NonNull VideoItem item,
            @NonNull VideoMetadata metadata,
            @NonNull ResolvedVideoCompressionSettings settings
    ) {
        if (cancelled) {
            finishBatch(true);
            return;
        }

        File tempOutput = new File(
                appContext.getCacheDir(),
                buildOutputName(metadata.getDisplayName()).replace(".mp4", "_working.mp4")
        );
        exportItemWithCodec(item, metadata, settings, tempOutput, settings.getTargetVideoMimeType());
    }

    @OptIn(markerClass = UnstableApi.class)
    @MainThread
    private void exportItemWithCodec(
            @NonNull VideoItem item,
            @NonNull VideoMetadata metadata,
            @NonNull ResolvedVideoCompressionSettings settings,
            @NonNull File tempOutput,
            @NonNull String videoMimeType
    ) {
        if (cancelled) {
            finishBatch(true);
            return;
        }

        if (tempOutput.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tempOutput.delete();
        }

        ImmutableList.Builder<Effect> videoEffectsBuilder = ImmutableList.builder();
        if (settings.getTargetShortSide() > 0) {
            videoEffectsBuilder.add(Presentation.createForShortSide(settings.getTargetShortSide()));
        }

        EditedMediaItem.Builder editedMediaItemBuilder = new EditedMediaItem.Builder(
                MediaItem.fromUri(metadata.getSourceUri())
        );
        editedMediaItemBuilder.setFrameRate(settings.getTargetFrameRate());
        editedMediaItemBuilder.setEffects(new Effects(
                ImmutableList.of(),
                videoEffectsBuilder.build()
        ));

        Composition composition = new Composition.Builder(
                new EditedMediaItemSequence(editedMediaItemBuilder.build())
        ).build();

        VideoEncoderSettings videoEncoderSettings = new VideoEncoderSettings.Builder()
                .setBitrate(settings.getTargetBitrate())
                .setiFrameIntervalSeconds(I_FRAME_INTERVAL_SECONDS)
                .build();

        DefaultEncoderFactory.Builder encoderFactoryBuilder = new DefaultEncoderFactory.Builder(appContext)
                .setEnableFallback(true)
                .setRequestedVideoEncoderSettings(videoEncoderSettings);
        if (settings.hasAudioTrack() && settings.getTargetAudioBitrate() > 0) {
            encoderFactoryBuilder.setRequestedAudioEncoderSettings(
                    new AudioEncoderSettings.Builder()
                            .setBitrate(settings.getTargetAudioBitrate())
                            .build()
            );
        }

        currentTransformer = new Transformer.Builder(appContext)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setVideoMimeType(videoMimeType)
                .setEncoderFactory(encoderFactoryBuilder.build())
                .addListener(new Transformer.Listener() {
                    @Override
                    public void onCompleted(Composition ignored, ExportResult exportResult) {
                        stopProgressPolling();
                        currentTransformer = null;
                        handleItemSuccess(item, metadata, settings, tempOutput);
                    }

                    @Override
                    public void onError(
                            Composition ignored,
                            ExportResult exportResult,
                            ExportException exportException
                    ) {
                        stopProgressPolling();
                        currentTransformer = null;
                        if (cancelled) {
                            finishBatch(true);
                            return;
                        }
                        if (shouldRetryWithSafeCodec(videoMimeType)) {
                            exportItemWithCodec(item, metadata, settings, tempOutput, CodecSupportUtils.MIME_AVC);
                            return;
                        }
                        handleItemFailure(item, exportException);
                    }
                })
                .build();

        startProgressPolling(item);
        currentTransformer.start(composition, tempOutput.getAbsolutePath());
    }

    private boolean shouldRetryWithSafeCodec(@Nullable String videoMimeType) {
        return videoMimeType != null
                && !CodecSupportUtils.MIME_AVC.equals(videoMimeType)
                && CodecSupportUtils.isCodecUsable(CodecSupportUtils.MIME_AVC);
    }

    private void handleItemSuccess(
            @NonNull VideoItem item,
            @NonNull VideoMetadata metadata,
            @NonNull ResolvedVideoCompressionSettings settings,
            @NonNull File tempOutput
    ) {
        ioExecutor.execute(() -> {
            try {
                String outputName = buildOutputName(metadata.getDisplayName());
                Uri outputUri = repository.saveCompressedVideo(tempOutput, outputName);
                long outputBytes = tempOutput.length();
                VideoCompressionResult result = VideoCompressionResult.success(
                        metadata.getDisplayName(),
                        metadata.getSourceUri(),
                        metadata.getSizeBytes(),
                        outputBytes,
                        metadata.getDurationMs(),
                        settings.getOutputWidth(),
                        settings.getOutputHeight(),
                        settings.getTargetBitrate(),
                        settings.getTargetFrameRate(),
                        outputUri,
                        outputName
                );
                //noinspection ResultOfMethodCallIgnored
                tempOutput.delete();
                mainHandler.post(() -> handleItemResult(result));
            } catch (IOException exception) {
                handleItemFailure(item, exception);
            }
        });
    }

    private void handleItemFailure(@NonNull VideoItem item, @NonNull Exception exception) {
        VideoCompressionResult result = VideoCompressionResult.failure(
                item.getDisplayName(),
                item.getUri(),
                item.getSizeBytes(),
                item.getDurationMs(),
                exception.getMessage() == null ? "Compression failed" : exception.getMessage()
        );
        mainHandler.post(() -> handleItemResult(result));
    }

    @MainThread
    private void handleItemResult(@NonNull VideoCompressionResult result) {
        if (cancelled) {
            finishBatch(true);
            return;
        }
        results.add(result);
        if (callback != null) {
            callback.onItemResult(result);
        }
        currentIndex += 1;
        if (currentIndex < queue.size()) {
            dispatchProgress(queue.get(currentIndex), 0, VideoCompressionStage.PREPARING);
        }
        startNextItem();
    }

    @MainThread
    private void finishBatch(boolean wasCancelled) {
        stopProgressPolling();
        running = false;
        currentTransformer = null;
        if (callback != null) {
            callback.onBatchFinished(new ArrayList<>(results), wasCancelled);
        }
    }

    @MainThread
    private void startProgressPolling(@NonNull VideoItem item) {
        stopProgressPolling();
        progressRunnable = new Runnable() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void run() {
                if (currentTransformer == null || cancelled || !running) {
                    return;
                }
                ProgressHolder progressHolder = new ProgressHolder();
                int progressState = currentTransformer.getProgress(progressHolder);
                int itemProgress = progressState == Transformer.PROGRESS_STATE_AVAILABLE
                        ? Math.max(0, Math.min(100, progressHolder.progress))
                        : 0;
                dispatchProgress(item, itemProgress, mapStage(itemProgress));
                mainHandler.postDelayed(this, 350L);
            }
        };
        mainHandler.post(progressRunnable);
    }

    @MainThread
    private void stopProgressPolling() {
        if (progressRunnable != null) {
            mainHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    @MainThread
    private void dispatchProgress(
            @NonNull VideoItem item,
            int itemProgressPercent,
            @NonNull VideoCompressionStage stage
    ) {
        if (callback == null) {
            return;
        }
        int completedCount = 0;
        int failedCount = 0;
        for (VideoCompressionResult result : results) {
            if (result.isSuccess()) {
                completedCount++;
            } else {
                failedCount++;
            }
        }
        float totalItems = Math.max(1, queue.size());
        int overallProgress = Math.min(
                100,
                Math.round(((currentIndex * 100f) + itemProgressPercent) / totalItems)
        );
        long elapsed = SystemClock.elapsedRealtime() - batchStartedAtMs;
        long estimatedRemainingMs = overallProgress > 0
                ? Math.max(0L, (elapsed * (100L - overallProgress)) / overallProgress)
                : -1L;

        callback.onProgress(new VideoCompressionProgress(
                Math.min(queue.size(), currentIndex + 1),
                queue.size(),
                completedCount,
                failedCount,
                itemProgressPercent,
                overallProgress,
                estimatedRemainingMs,
                item.getDisplayName(),
                stage
        ));
    }

    @NonNull
    private VideoCompressionStage mapStage(int itemProgressPercent) {
        if (itemProgressPercent >= 99) {
            return VideoCompressionStage.SAVING;
        }
        if (itemProgressPercent >= 88) {
            return VideoCompressionStage.MUXING;
        }
        if (itemProgressPercent >= 18) {
            return VideoCompressionStage.ENCODING;
        }
        if (itemProgressPercent > 0) {
            return VideoCompressionStage.EXTRACTING;
        }
        return VideoCompressionStage.PREPARING;
    }

    @NonNull
    private String buildOutputName(@NonNull String originalName) {
        String baseName = originalName;
        int extensionIndex = originalName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = originalName.substring(0, extensionIndex);
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return baseName + "_compressed_" + timestamp + ".mp4";
    }
}

