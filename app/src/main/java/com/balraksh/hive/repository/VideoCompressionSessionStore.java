package com.balraksh.hive.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.balraksh.hive.data.VideoItem;
import com.balraksh.hive.video.VideoCompressionManager;
import com.balraksh.hive.video.VideoCompressionProgress;
import com.balraksh.hive.video.VideoCompressionRequest;
import com.balraksh.hive.video.VideoCompressionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class VideoCompressionSessionStore {

    public enum State {
        IDLE,
        RUNNING,
        COMPLETED,
        CANCELLED
    }

    public interface Listener {
        void onSessionUpdated();
    }

    private static VideoCompressionSessionStore instance;

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final VideoCompressionManager compressionManager;

    private List<VideoItem> selectedVideos = new ArrayList<>();
    private List<VideoCompressionResult> results = new ArrayList<>();
    private VideoCompressionRequest currentRequest;
    private VideoCompressionProgress latestProgress;
    private State state = State.IDLE;
    private boolean historyRecorded;

    private VideoCompressionSessionStore(Context context) {
        compressionManager = new VideoCompressionManager(context);
    }

    public static synchronized VideoCompressionSessionStore getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new VideoCompressionSessionStore(context.getApplicationContext());
        }
        return instance;
    }

    public synchronized void resetSelection() {
        selectedVideos = new ArrayList<>();
        currentRequest = null;
        results = new ArrayList<>();
        latestProgress = null;
        state = State.IDLE;
        historyRecorded = false;
        notifyListeners();
    }

    public synchronized void setSelectedVideos(@NonNull List<VideoItem> videos) {
        selectedVideos = new ArrayList<>(videos);
        notifyListeners();
    }

    @NonNull
    public synchronized List<VideoItem> getSelectedVideos() {
        return new ArrayList<>(selectedVideos);
    }

    public synchronized void startCompression(@NonNull VideoCompressionRequest request) {
        currentRequest = request;
        results = new ArrayList<>();
        latestProgress = null;
        state = State.RUNNING;
        historyRecorded = false;
        notifyListeners();
        compressionManager.startCompression(selectedVideos, request, new VideoCompressionManager.Callback() {
            @Override
            public void onProgress(@NonNull VideoCompressionProgress progress) {
                synchronized (VideoCompressionSessionStore.this) {
                    latestProgress = progress;
                }
                notifyListeners();
            }

            @Override
            public void onItemResult(@NonNull VideoCompressionResult result) {
                synchronized (VideoCompressionSessionStore.this) {
                    results = new ArrayList<>(results);
                    results.add(result);
                }
                notifyListeners();
            }

            @Override
            public void onBatchFinished(@NonNull List<VideoCompressionResult> batchResults, boolean cancelled) {
                synchronized (VideoCompressionSessionStore.this) {
                    results = new ArrayList<>(batchResults);
                    state = cancelled ? State.CANCELLED : State.COMPLETED;
                }
                notifyListeners();
            }
        });
    }

    public synchronized void cancelCompression() {
        if (state == State.RUNNING) {
            compressionManager.cancel();
            state = State.CANCELLED;
            notifyListeners();
        }
    }

    public synchronized boolean isRunning() {
        return state == State.RUNNING;
    }

    @Nullable
    public synchronized VideoCompressionProgress getLatestProgress() {
        return latestProgress;
    }

    @NonNull
    public synchronized List<VideoCompressionResult> getResults() {
        return new ArrayList<>(results);
    }

    @Nullable
    public synchronized VideoCompressionRequest getCurrentRequest() {
        return currentRequest;
    }

    @NonNull
    public synchronized State getState() {
        return state;
    }

    public synchronized boolean isHistoryRecorded() {
        return historyRecorded;
    }

    public synchronized void setHistoryRecorded(boolean historyRecorded) {
        this.historyRecorded = historyRecorded;
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            listener.onSessionUpdated();
        }
    }
}

