package com.balraksh.hive.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.balraksh.hive.data.VideoCompressionHistoryEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VideoCompressionPreferences {

    private static final String PREFS_NAME = "safkaro_video_history";
    private static final String KEY_HISTORY = "key_video_history";
    private static final int MAX_HISTORY_ITEMS = 20;

    private final SharedPreferences preferences;

    public VideoCompressionPreferences(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void addHistory(VideoCompressionHistoryEntry entry) {
        List<VideoCompressionHistoryEntry> history = getHistory();
        history.add(0, entry);
        while (history.size() > MAX_HISTORY_ITEMS) {
            history.remove(history.size() - 1);
        }
        preferences.edit()
                .putString(KEY_HISTORY, toJson(history).toString())
                .apply();
    }

    public List<VideoCompressionHistoryEntry> getHistory() {
        String raw = preferences.getString(KEY_HISTORY, "[]");
        List<VideoCompressionHistoryEntry> history = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                history.add(new VideoCompressionHistoryEntry(
                        object.optLong("timestamp"),
                        object.optInt("compressedCount"),
                        object.optLong("savedBytes")
                ));
            }
        } catch (JSONException ignored) {
        }
        return history;
    }

    public void clear() {
        preferences.edit().remove(KEY_HISTORY).apply();
    }

    private JSONArray toJson(List<VideoCompressionHistoryEntry> history) {
        JSONArray array = new JSONArray();
        for (VideoCompressionHistoryEntry entry : history) {
            JSONObject object = new JSONObject();
            try {
                object.put("timestamp", entry.getTimestampMillis());
                object.put("compressedCount", entry.getCompressedCount());
                object.put("savedBytes", entry.getSavedBytes());
            } catch (JSONException ignored) {
            }
            array.put(object);
        }
        return array;
    }
}
