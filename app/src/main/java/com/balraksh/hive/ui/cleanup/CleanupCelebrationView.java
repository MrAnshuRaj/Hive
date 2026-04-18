package com.balraksh.hive.ui.cleanup;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.balraksh.hive.R;

import java.util.Random;

public class CleanupCelebrationView extends View {

    private static final int PARTICLE_COUNT = 42;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Particle[] particles = new Particle[PARTICLE_COUNT];
    private final Random random = new Random();
    private int[] palette;

    private ValueAnimator animator;
    private float progress;

    public CleanupCelebrationView(Context context) {
        this(context, null);
    }

    public CleanupCelebrationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        palette = new int[]{
                getContext().getColor(R.color.color_cleanup_success_green),
                getContext().getColor(R.color.color_cleanup_success_accent),
                getContext().getColor(R.color.white)
        };
        initParticles();
    }

    public void setPalette(int... colors) {
        if (colors == null || colors.length == 0) {
            return;
        }
        palette = colors.clone();
        for (Particle particle : particles) {
            particle.color = palette[random.nextInt(palette.length)];
        }
        invalidate();
    }

    private void initParticles() {
        for (int index = 0; index < particles.length; index++) {
            particles[index] = new Particle();
            resetParticle(particles[index], true);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2400L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
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

        for (Particle particle : particles) {
            float traveled = ((progress + particle.offset) % 1f);
            float x = particle.startX * width + particle.drift * width * traveled;
            float y = (particle.startY + particle.travelDistance * traveled) * height;
            if (y > height + particle.size * 2f) {
                resetParticle(particle, false);
                continue;
            }

            paint.setColor(particle.color);
            paint.setAlpha((int) (particle.alpha * 255));
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(particle.rotation + (360f * traveled * particle.spin));
            if (particle.round) {
                canvas.drawCircle(0f, 0f, particle.size * 0.5f, paint);
            } else {
                rect.set(-particle.size, -particle.size * 0.45f, particle.size, particle.size * 0.45f);
                canvas.drawRoundRect(rect, particle.size * 0.18f, particle.size * 0.18f, paint);
            }
            canvas.restore();
        }
    }

    private void resetParticle(Particle particle, boolean initial) {
        particle.startX = random.nextFloat();
        particle.startY = initial ? random.nextFloat() * 0.85f : -0.15f - random.nextFloat() * 0.2f;
        particle.travelDistance = 1.15f + random.nextFloat() * 0.3f;
        particle.size = dp(2f + random.nextFloat() * 4f);
        particle.offset = random.nextFloat();
        particle.drift = -0.08f + random.nextFloat() * 0.16f;
        particle.rotation = random.nextFloat() * 360f;
        particle.spin = 0.5f + random.nextFloat() * 1.2f;
        particle.round = random.nextBoolean();
        particle.alpha = 0.4f + random.nextFloat() * 0.45f;

        particle.color = palette[random.nextInt(palette.length)];
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static final class Particle {
        float startX;
        float startY;
        float travelDistance;
        float size;
        float offset;
        float drift;
        float rotation;
        float spin;
        float alpha;
        boolean round;
        int color;
    }
}
