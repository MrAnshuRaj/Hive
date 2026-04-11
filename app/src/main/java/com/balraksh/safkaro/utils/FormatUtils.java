package com.balraksh.safkaro.utils;

import java.text.DecimalFormat;

public final class FormatUtils {

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final long GB = MB * 1024L;

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
}
