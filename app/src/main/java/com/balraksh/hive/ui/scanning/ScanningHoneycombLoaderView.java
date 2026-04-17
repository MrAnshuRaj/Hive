package com.balraksh.hive.ui.scanning;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

public class ScanningHoneycombLoaderView extends View {

    private static final int HEX_COUNT = 6;
    private static final float[] HEX_ANGLES = new float[]{-90f, -30f, 30f, 90f, 150f, 210f};

    private final Paint hexPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator pulseAnimator;
    private float animationProgress;

    public ScanningHoneycombLoaderView(Context context) {
        this(context, null);
    }

    public ScanningHoneycombLoaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        hexPaint.setStyle(Paint.Style.FILL);
        setWillNotDraw(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimationLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimationLoop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredSize = (int) (getResources().getDisplayMetrics().density * 180f);
        int width = resolveSize(desiredSize, widthMeasureSpec);
        int height = resolveSize(desiredSize, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float orbitRadius = Math.min(getWidth(), getHeight()) * 0.29f;
        float baseRadius = Math.min(getWidth(), getHeight()) * 0.09f;

        for (int i = 0; i < HEX_COUNT; i++) {
            float phase = (animationProgress + (i / (float) HEX_COUNT)) % 1f;
            float pulse = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin((phase * Math.PI * 2f) - Math.PI));
            float angle = (float) Math.toRadians(HEX_ANGLES[i]);
            float hexCenterX = centerX + (float) Math.cos(angle) * orbitRadius;
            float hexCenterY = centerY + (float) Math.sin(angle) * orbitRadius;
            float radius = baseRadius * (0.88f + (pulse * 0.22f));
            int alpha = Math.min(255, Math.max(0, Math.round(70f + (pulse * 150f))));
            hexPaint.setColor(Color.argb(alpha, 255, 177, 23));
            canvas.drawPath(buildHexagon(hexCenterX, hexCenterY, radius), hexPaint);
        }
    }

    private void startAnimationLoop() {
        stopAnimationLoop();
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1600L);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.RESTART);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    private void stopAnimationLoop() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
    }

    private Path buildHexagon(float centerX, float centerY, float radius) {
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
}
