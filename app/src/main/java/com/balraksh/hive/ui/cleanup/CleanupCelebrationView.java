package com.balraksh.hive.ui.cleanup;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.balraksh.hive.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CleanupCelebrationView extends View {

    private static final long SIDE_BURST_ONE_MS = 300L;
    private static final long SIDE_BURST_TWO_MS = 600L;
    private static final long CENTER_BURST_MS = 1100L;
    private static final long SIDE_DURATION_MS = 2200L;
    private static final long CENTER_DURATION_MS = 3000L;
    private static final long TOTAL_DURATION_MS = CENTER_BURST_MS + CENTER_DURATION_MS;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private final DecelerateInterpolator alphaInterpolator = new DecelerateInterpolator();
    private int[] palette;

    private ValueAnimator animator;
    private float elapsedMs;
    private boolean particlesReady;

    public CleanupCelebrationView(Context context) {
        this(context, null);
    }

    public CleanupCelebrationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        palette = new int[]{
                getContext().getColor(R.color.color_cleanup_success_green),
                Color.rgb(52, 211, 153),
                Color.rgb(5, 150, 105),
                getContext().getColor(R.color.white)
        };
    }

    public void setPalette(int... colors) {
        if (colors == null || colors.length == 0) {
            return;
        }
        palette = colors.clone();
        particlesReady = false;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(this::startCelebration);
    }

    private void startCelebration() {
        if (!isAttachedToWindow()) {
            return;
        }
        buildParticles();
        elapsedMs = 0f;
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, TOTAL_DURATION_MS);
        animator.setDuration(TOTAL_DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            elapsedMs = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                elapsedMs = TOTAL_DURATION_MS;
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        if (!particlesReady) {
            buildParticles();
        }

        for (Particle particle : particles) {
            float localMs = elapsedMs - particle.startMs;
            if (localMs < 0f || localMs > particle.durationMs) {
                continue;
            }

            float seconds = localMs / 1000f;
            float progress = localMs / particle.durationMs;
            float easedAlpha = 1f - alphaInterpolator.getInterpolation(Math.max(0f, progress - 0.2f) / 0.8f);
            float x = particle.originX + (particle.velocityX * seconds);
            float y = particle.originY + (particle.velocityY * seconds) + (0.5f * particle.gravity * seconds * seconds);

            paint.setColor(particle.color);
            paint.setAlpha((int) (particle.alpha * easedAlpha * 255));
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(particle.rotation + (360f * progress * particle.spin));
            if (particle.round) {
                canvas.drawCircle(0f, 0f, particle.size * 0.5f, paint);
            } else {
                rect.set(-particle.size, -particle.size * 0.45f, particle.size, particle.size * 0.45f);
                canvas.drawRoundRect(rect, particle.size * 0.18f, particle.size * 0.18f, paint);
            }
            canvas.restore();
        }
    }

    private void buildParticles() {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        particles.clear();
        addBurst(SIDE_BURST_ONE_MS, 0.2f * width, 0.7f * height, 60f, 60f, 88, SIDE_DURATION_MS);
        addBurst(SIDE_BURST_TWO_MS, 0.8f * width, 0.7f * height, 120f, 60f, 88, SIDE_DURATION_MS);
        addBurst(CENTER_BURST_MS, 0.5f * width, 0.4f * height, 90f, 120f, 170, CENTER_DURATION_MS);
        particlesReady = true;
    }

    private void addBurst(
            long startMs,
            float originX,
            float originY,
            float angleDegrees,
            float spreadDegrees,
            int count,
            long durationMs
    ) {
        for (int index = 0; index < count; index++) {
            Particle particle = new Particle();
            float angle = (float) Math.toRadians(angleDegrees - (spreadDegrees * 0.5f) + (random.nextFloat() * spreadDegrees));
            float velocity = dp(260f + random.nextFloat() * 430f);
            particle.startMs = startMs + random.nextInt(120);
            particle.durationMs = durationMs - random.nextInt(420);
            particle.originX = originX;
            particle.originY = originY;
            particle.velocityX = (float) Math.cos(angle) * velocity;
            particle.velocityY = -(float) Math.sin(angle) * velocity;
            particle.gravity = dp(560f + random.nextFloat() * 240f);
            particle.size = dp(2f + random.nextFloat() * 4.5f);
            particle.color = palette[random.nextInt(palette.length)];
            particle.alpha = 0.55f + random.nextFloat() * 0.45f;
            particle.round = random.nextFloat() > 0.62f;
            particle.rotation = random.nextFloat() * 360f;
            particle.spin = 0.45f + random.nextFloat() * 1.6f;
            particles.add(particle);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static final class Particle {
        long startMs;
        long durationMs;
        float originX;
        float originY;
        float velocityX;
        float velocityY;
        float gravity;
        float size;
        float rotation;
        float spin;
        float alpha;
        boolean round;
        int color;
    }
}
