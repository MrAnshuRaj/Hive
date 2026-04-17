package com.balraksh.hive.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.balraksh.hive.data.CleanupHistoryEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CleanupPreferences {

    private static final String PREFS_NAME = "safkaro_cleanup_prefs";
    private static final String KEY_TOTAL_FREED = "key_total_freed";
    private static final String KEY_LAST_SCAN_POTENTIAL = "key_last_scan_potential";
    private static final String KEY_LAST_DUPLICATE_COUNT = "key_last_duplicate_count";
    private static final String KEY_LAST_DUPLICATE_BYTES = "key_last_duplicate_bytes";
    private static final String KEY_LAST_SIMILAR_COUNT = "key_last_similar_count";
    private static final String KEY_LAST_SIMILAR_BYTES = "key_last_similar_bytes";
    private static final String KEY_HISTORY = "key_history";
    private static final int MAX_HISTORY_ITEMS = 20;

    private final SharedPreferences preferences;

    public CleanupPreferences(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public long getTotalFreedBytes() {
        return preferences.getLong(KEY_TOTAL_FREED, 0L);
    }

    public void addCleanupHistory(CleanupHistoryEntry entry) {
        long total = getTotalFreedBytes() + entry.getFreedBytes();
        List<CleanupHistoryEntry> history = getHistory();
        history.add(0, entry);
        while (history.size() > MAX_HISTORY_ITEMS) {
            history.remove(history.size() - 1);
        }
        preferences.edit()
                .putLong(KEY_TOTAL_FREED, total)
                .putString(KEY_HISTORY, toJson(history).toString())
                .apply();
    }

    public long getLastScanPotentialBytes() {
        return preferences.getLong(KEY_LAST_SCAN_POTENTIAL, 0L);
    }

    public void setLastScanPotentialBytes(long bytes) {
        preferences.edit().putLong(KEY_LAST_SCAN_POTENTIAL, bytes).apply();
    }

    public void setLastScanInsights(int duplicateCount, long duplicateBytes, int similarCount, long similarBytes) {
        preferences.edit()
                .putInt(KEY_LAST_DUPLICATE_COUNT, duplicateCount)
                .putLong(KEY_LAST_DUPLICATE_BYTES, duplicateBytes)
                .putInt(KEY_LAST_SIMILAR_COUNT, similarCount)
                .putLong(KEY_LAST_SIMILAR_BYTES, similarBytes)
                .apply();
    }

    public int getLastDuplicateCount() {
        return preferences.getInt(KEY_LAST_DUPLICATE_COUNT, 0);
    }

    public long getLastDuplicateBytes() {
        return preferences.getLong(KEY_LAST_DUPLICATE_BYTES, 0L);
    }

    public int getLastSimilarCount() {
        return preferences.getInt(KEY_LAST_SIMILAR_COUNT, 0);
    }

    public long getLastSimilarBytes() {
        return preferences.getLong(KEY_LAST_SIMILAR_BYTES, 0L);
    }

    public List<CleanupHistoryEntry> getHistory() {
        String raw = preferences.getString(KEY_HISTORY, "[]");
        List<CleanupHistoryEntry> entries = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                entries.add(new CleanupHistoryEntry(
                        object.optLong("timestamp"),
                        object.optInt("deletedCount"),
                        object.optLong("freedBytes")
                ));
            }
        } catch (JSONException ignored) {
        }
        return entries;
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    private JSONArray toJson(List<CleanupHistoryEntry> entries) {
        JSONArray array = new JSONArray();
        for (CleanupHistoryEntry entry : entries) {
            JSONObject object = new JSONObject();
            try {
                object.put("timestamp", entry.getTimestampMillis());
                object.put("deletedCount", entry.getDeletedCount());
                object.put("freedBytes", entry.getFreedBytes());
            } catch (JSONException ignored) {
            }
            array.put(object);
        }
        return array;
    }
}

