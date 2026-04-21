package com.herocounter.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fullscreen fireworks celebration view.
 * Drawn entirely with Canvas — no external libraries, no network.
 * Shows for ~2 seconds then calls onDone.runnable.
 */
public class FireworksView extends View {

    public interface OnDoneListener { void onDone(); }

    private static final int   BURST_COUNT    = 7;
    private static final int   PARTICLES_PER  = 28;
    private static final long  DURATION_MS    = 2200;
    private static final int[] COLORS = {
        0xFFFF4444, 0xFFFFAA00, 0xFF44FF44, 0xFF4488FF,
        0xFFFF44FF, 0xFFFFFF44, 0xFF00FFCC, 0xFFFF8800
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Particle> particles = new ArrayList<>();
    private final Random rng = new Random();
    private OnDoneListener listener;
    private ValueAnimator animator;

    public FireworksView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void start(OnDoneListener listener) {
        this.listener = listener;
        particles.clear();

        post(() -> {
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) { w = 1080; h = 1920; }

            // Create bursts at random positions biased toward center
            for (int b = 0; b < BURST_COUNT; b++) {
                float cx = w * (0.15f + rng.nextFloat() * 0.7f);
                float cy = h * (0.10f + rng.nextFloat() * 0.5f);
                int color = COLORS[rng.nextInt(COLORS.length)];
                float delay = rng.nextFloat() * 0.6f; // stagger 0–600 ms

                for (int p = 0; p < PARTICLES_PER; p++) {
                    double angle = Math.random() * Math.PI * 2;
                    float speed  = 8f + rng.nextFloat() * 18f;
                    float vx     = (float)(Math.cos(angle) * speed);
                    float vy     = (float)(Math.sin(angle) * speed);
                    float size   = 5f + rng.nextFloat() * 7f;
                    // Slight color variation per particle
                    int pColor   = vary(color);
                    particles.add(new Particle(cx, cy, vx, vy, size, pColor, delay));
                }
            }

            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(DURATION_MS);
            animator.setInterpolator(new AccelerateInterpolator(0.5f));
            animator.addUpdateListener(a -> invalidate());
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) {
                    if (listener != null) listener.onDone();
                }
            });
            animator.start();
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (animator == null) return;
        float t = animator.getAnimatedFraction(); // 0→1 over 2.2s

        for (Particle p : particles) {
            float pt = t - p.delay;
            if (pt <= 0f) continue;

            // Physics: position + gravity
            float x = p.x0 + p.vx * pt * 60f;
            float y = p.y0 + p.vy * pt * 60f + 120f * pt * pt; // gravity

            // Fade out in last 40% of life
            float alpha = pt < 0.6f ? 1f : 1f - (pt - 0.6f) / 0.4f;
            alpha = Math.max(0f, Math.min(1f, alpha));

            // Shrink slightly
            float size = p.size * (1f - pt * 0.3f);

            paint.setColor(p.color);
            paint.setAlpha((int)(alpha * 255));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, size, paint);

            // Sparkle trail
            paint.setAlpha((int)(alpha * 80));
            canvas.drawCircle(x - p.vx * pt * 8f, y - p.vy * pt * 8f, size * 0.5f, paint);
        }
    }

    private int vary(int base) {
        int r = Math.min(255, Math.max(0, Color.red(base)   + rng.nextInt(40) - 20));
        int g = Math.min(255, Math.max(0, Color.green(base) + rng.nextInt(40) - 20));
        int b = Math.min(255, Math.max(0, Color.blue(base)  + rng.nextInt(40) - 20));
        return Color.rgb(r, g, b);
    }

    public void cancel() {
        if (animator != null) animator.cancel();
    }

    private static class Particle {
        float x0, y0, vx, vy, size, delay;
        int color;
        Particle(float x0, float y0, float vx, float vy,
                 float size, int color, float delay) {
            this.x0 = x0; this.y0 = y0;
            this.vx = vx; this.vy = vy;
            this.size = size; this.color = color;
            this.delay = delay;
        }
    }
}
