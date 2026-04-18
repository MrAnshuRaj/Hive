package com.balraksh.hive.utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FormatUtils {

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final long GB = MB * 1024L;
    private static final SimpleDateFormat SHORT_DATE_FORMAT =
            new SimpleDateFormat("MMM d", Locale.getDefault());

    private FormatUtils() {
    }

    public static String formatStorage(long bytes) {
        if (bytes >= GB) {
            return new DecimalFormat("0.0").format((double) bytes / (double) GB) + " GB";
        }
        if (bytes >= MB) {
            double value = (double) bytes / (double) MB;
            String pattern = value >= 100 ? "0" : "0.0";
            return new DecimalFormat(pattern).format(value) + " MB";
        }
        if (bytes >= KB) {
            return new DecimalFormat("0").format((double) bytes / (double) KB) + " KB";
        }
        return bytes + " B";
    }

    public static String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0L, durationMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes >= 60L) {
            long hours = minutes / 60L;
            long remainingMinutes = minutes % 60L;
            return hours + ":" + String.format("%02d:%02d", remainingMinutes, seconds);
        }
        return minutes + ":" + String.format("%02d", seconds);
    }

    public static String formatPercentage(float fraction) {
        return new DecimalFormat("0").format(fraction * 100f) + "%";
    }

    public static String formatShortDate(long timeMs) {
        return SHORT_DATE_FORMAT.format(new Date(timeMs));
    }
}

