package com.balraksh.safkaro.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ImageHashUtils {

    private static final int HASH_WIDTH = 9;
    private static final int HASH_HEIGHT = 8;

    private ImageHashUtils() {
    }

    public static String sha256(ContentResolver resolver, Uri uri) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = resolver.openInputStream(uri)) {
                if (inputStream == null) {
                    throw new IOException("Unable to open stream for " + uri);
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hashed = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 unavailable", exception);
        }
    }

    public static long dHash(Context context, Uri uri) throws IOException {
        Bitmap bitmap = decodeSampledBitmap(context, uri, 96, 96);
        if (bitmap == null) {
            throw new IOException("Unable to decode bitmap");
        }
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, HASH_WIDTH, HASH_HEIGHT, true);
        if (scaled != bitmap) {
            bitmap.recycle();
        }
        long hash = 0L;
        int bitIndex = 0;
        for (int y = 0; y < HASH_HEIGHT; y++) {
            for (int x = 0; x < HASH_WIDTH - 1; x++) {
                int left = scaled.getPixel(x, y);
                int right = scaled.getPixel(x + 1, y);
                if (luma(left) > luma(right)) {
                    hash |= (1L << bitIndex);
                }
                bitIndex++;
            }
        }
        scaled.recycle();
        return hash;
    }

    public static int hammingDistance(long first, long second) {
        return Long.bitCount(first ^ second);
    }

    private static Bitmap decodeSampledBitmap(Context context, Uri uri, int reqWidth, int reqHeight)
            throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                return null;
            }
            BitmapFactory.decodeStream(stream, null, bounds);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                return null;
            }
            return BitmapFactory.decodeStream(stream, null, options);
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private static int luma(int color) {
        int red = (color >> 16) & 0xff;
        int green = (color >> 8) & 0xff;
        int blue = color & 0xff;
        return (red * 299 + green * 587 + blue * 114) / 1000;
    }
}
