package com.balraksh.hive.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.balraksh.hive.data.StorageCategoryUsage;
import com.balraksh.hive.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StorageHoneycombView extends View {

    private static final String[] DEFAULT_LABELS = new String[]{
            "Videos", "Photos", "Docs", "Music", "Apps", "Other"
    };

    private static final float SQRT_3 = (float) Math.sqrt(3d);
    private static final float APOTHEM_RATIO = SQRT_3 / 2f;
    private static final float CENTER_RADIUS_FACTOR = 1.16f;
    private static final float RING_GAP_FACTOR = 0.12f;

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint percentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();

    private long totalStorageBytes;
    private long usedStorageBytes;
    private long recommendedFreeBytes;
    private List<StorageCategoryUsage> categories = new ArrayList<>();

    public StorageHoneycombView(Context context) {
        this(context, null);
    }

    public StorageHoneycombView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        int horizontalPadding = (int) dp(10f);
        int verticalPadding = (int) dp(8f);
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.8f));

        fillPaint.setStyle(Paint.Style.FILL);

        percentPaint.setColor(Color.WHITE);
        percentPaint.setTextAlign(Paint.Align.CENTER);
        percentPaint.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL));

        valuePaint.setColor(0xFFFAC75D);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));

        titlePaint.setColor(0xFFD6C9B8);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));

        centerValuePaint.setColor(0xFFFFB117);
        centerValuePaint.setTextAlign(Paint.Align.CENTER);
        centerValuePaint.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL));

        centerLabelPaint.setColor(0xFFC9B8A6);
        centerLabelPaint.setTextAlign(Paint.Align.CENTER);
        centerLabelPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    }

    public void setData(long totalStorageBytes, long usedStorageBytes, long recommendedFreeBytes, List<StorageCategoryUsage> categories) {
        this.totalStorageBytes = totalStorageBytes;
        this.usedStorageBytes = usedStorageBytes;
        this.recommendedFreeBytes = recommendedFreeBytes;
        this.categories = new ArrayList<>(categories);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            width = (int) dp(320f);
        }

        int desiredHeight = (int) (width * 0.82f);
        desiredHeight = Math.max((int) dp(220f), desiredHeight);
        desiredHeight = Math.min((int) dp(290f), desiredHeight);

        int resolvedWidth = resolveSize(width, widthMeasureSpec);
        int resolvedHeight = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(resolvedWidth, resolvedHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (width <= 0f || height <= 0f) {
            return;
        }

        LayoutSpec spec = computeLayout(width, height);
        applyTextSizing(spec.outerRadius, spec.centerRadius);

        drawCategoryHex(canvas, spec.centerX, spec.centerY - spec.verticalDistance, spec.outerRadius, getCategory(0, 0xFFFFB117));
        drawCategoryHex(canvas, spec.centerX + spec.diagonalDistanceX, spec.centerY - spec.diagonalDistanceY, spec.outerRadius, getCategory(1, 0xFFFF9D43));
        drawCategoryHex(canvas, spec.centerX + spec.diagonalDistanceX, spec.centerY + spec.diagonalDistanceY, spec.outerRadius, getCategory(2, 0xFFE39B63));
        drawCategoryHex(canvas, spec.centerX, spec.centerY + spec.verticalDistance, spec.outerRadius, getCategory(3, 0xFFF5C324));
        drawCategoryHex(canvas, spec.centerX - spec.diagonalDistanceX, spec.centerY + spec.diagonalDistanceY, spec.outerRadius, getCategory(4, 0xFFE7C154));
        drawCategoryHex(canvas, spec.centerX - spec.diagonalDistanceX, spec.centerY - spec.diagonalDistanceY, spec.outerRadius, getCategory(5, 0xFFE7D28D));
        drawCenterHex(canvas, spec.centerX, spec.centerY, spec.centerRadius);
    }

    private LayoutSpec computeLayout(float availableWidth, float availableHeight) {
        float maxOuterFromWidth = availableWidth / 5.2f;
        float maxOuterFromHeight = availableHeight / 5.95f;
        float outerRadius = Math.min(maxOuterFromWidth, maxOuterFromHeight);
        outerRadius = Math.max(dp(34f), outerRadius);

        float centerRadius = outerRadius * CENTER_RADIUS_FACTOR;
        float ringGap = Math.max(dp(4f), outerRadius * RING_GAP_FACTOR);
        float radialDistance = (centerRadius * APOTHEM_RATIO) + (outerRadius * APOTHEM_RATIO) + ringGap;

        float centerX = getPaddingLeft() + (availableWidth / 2f);
        float centerY = getPaddingTop() + (availableHeight / 2f) - dp(2f);
        float diagonalDistanceX = radialDistance * 0.8660254f;
        float diagonalDistanceY = radialDistance * 0.5f;

        return new LayoutSpec(centerX, centerY, outerRadius, centerRadius, radialDistance, diagonalDistanceX, diagonalDistanceY);
    }

    private void applyTextSizing(float outerRadiusPx, float centerRadiusPx) {
        float density = getResources().getDisplayMetrics().density;
        float outerDp = outerRadiusPx / density;
        float centerDp = centerRadiusPx / density;
        float outerScale = clamp(outerDp / 44f, 0.86f, 1.12f);
        float centerScale = clamp(centerDp / 52f, 0.92f, 1.18f);

        percentPaint.setTextSize(sp(clamp(12f * outerScale, 10.5f, 14f)));
        valuePaint.setTextSize(sp(clamp(11f * outerScale, 10f, 13f)));
        titlePaint.setTextSize(sp(clamp(10f * outerScale, 9f, 12f)));
        centerValuePaint.setTextSize(sp(clamp(22f * centerScale, 18f, 28f)));
        centerLabelPaint.setTextSize(sp(clamp(11f * centerScale, 10f, 13.5f)));
    }

    private void drawCategoryHex(Canvas canvas, float centerX, float centerY, float radius, StorageCategoryUsage category) {
        int accentColor = category.getAccentColor();
        Path hexPath = buildPointyHexagon(centerX, centerY, radius);

        fillPaint.setColor(adjustAlpha(accentColor, 0.15f));
        canvas.drawPath(hexPath, fillPaint);

        strokePaint.setColor(accentColor);
        canvas.drawPath(hexPath, strokePaint);

        long categoryBytes = Math.max(0L, category.getBytes());
        int percentage = usedStorageBytes > 0L
                ? (int) Math.round((categoryBytes * 100d) / Math.max(1d, usedStorageBytes))
                : 0;

        percentPaint.setColor(accentColor);
        valuePaint.setColor(accentColor);

        float percentY = centerY - (radius * 0.26f);
        float valueY = centerY + (radius * 0.01f);
        float labelY = centerY + (radius * 0.34f);

        drawCenteredText(canvas, percentage + "%", centerX, percentY, percentPaint);
        drawCenteredText(canvas, compactStorage(categoryBytes), centerX, valueY, valuePaint);
        drawCenteredText(canvas, category.getLabel(), centerX, labelY, titlePaint);
    }

    private void drawCenterHex(Canvas canvas, float centerX, float centerY, float radius) {
        int accentColor = 0xFFFFB117;
        Path hexPath = buildPointyHexagon(centerX, centerY, radius);
        fillPaint.setColor(adjustAlpha(accentColor, 0.12f));
        canvas.drawPath(hexPath, fillPaint);

        strokePaint.setColor(accentColor);
        canvas.drawPath(hexPath, strokePaint);

        drawCenteredText(canvas, compactStorage(usedStorageBytes), centerX, centerY - (radius * 0.14f), centerValuePaint);
        drawCenteredText(canvas, "of " + compactStorage(totalStorageBytes) + " used", centerX, centerY + (radius * 0.16f), centerLabelPaint);
        drawCenteredText(canvas, "Keep " + compactStorage(recommendedFreeBytes) + " free", centerX, centerY + (radius * 0.42f), titlePaint);
    }

    private Path buildPointyHexagon(float centerX, float centerY, float radius) {
        Path path = new Path();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians((60d * i) - 90d);
            float x = (float) (centerX + radius * Math.cos(angle));
            float y = (float) (centerY + radius * Math.sin(angle));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        return path;
    }

    private void drawCenteredText(Canvas canvas, String text, float centerX, float baselineY, Paint paint) {
        paint.getTextBounds(text, 0, text.length(), textBounds);
        float textY = baselineY - ((textBounds.top + textBounds.bottom) / 2f);
        canvas.drawText(text, centerX, textY, paint);
    }

    private StorageCategoryUsage getCategory(int index, int fallbackColor) {
        if (index < categories.size()) {
            return categories.get(index);
        }
        return new StorageCategoryUsage(DEFAULT_LABELS[index], 0L, fallbackColor);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.min(255, Math.max(0, Math.round(Color.alpha(color) * factor)));
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private String compactStorage(long bytes) {
        return FormatUtils.formatStorage(bytes)
                .replace(" GB", "GB")
                .replace(" MB", "MB")
                .replace(" KB", "KB");
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    private static final class LayoutSpec {
        final float centerX;
        final float centerY;
        final float outerRadius;
        final float centerRadius;
        final float verticalDistance;
        final float diagonalDistanceX;
        final float diagonalDistanceY;

        LayoutSpec(float centerX, float centerY, float outerRadius, float centerRadius,
                   float verticalDistance, float diagonalDistanceX, float diagonalDistanceY) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.outerRadius = outerRadius;
            this.centerRadius = centerRadius;
            this.verticalDistance = verticalDistance;
            this.diagonalDistanceX = diagonalDistanceX;
            this.diagonalDistanceY = diagonalDistanceY;
        }
    }
}
