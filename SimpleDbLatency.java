package com.example.app.utils;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleDbLatency {
    private static final AtomicBoolean enabled = new AtomicBoolean(true);

    private static int minDelay = 400;
    private static int maxDelay = 500;

    private static final Random random = new Random();

    private static long totalLatencyApplied = 0;
    private static int latencyCount = 0;

    public static void enable(int min, int max) {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("Invalid latency range: min must be >= 0 and max must be >= min");
        }

        minDelay = min;
        maxDelay = max;
        enabled.set(true);

        totalLatencyApplied = 0;
        latencyCount = 0;

        System.out.println("Database latency simulation enabled: " + min + "-" + max + "ms");
    }
    public static void enable() {
        enable(50, 100);
    }

    public static void disable() {
        enabled.set(false);

        if (latencyCount > 0) {
            double avgLatency = (double) totalLatencyApplied / latencyCount;
            System.out.println("Database latency simulation disabled. Stats: " +
                    latencyCount + " delays applied, " +
                    String.format("%.2f", avgLatency) + "ms average");
        } else {
            System.out.println("Database latency simulation disabled. No latency was applied.");
        }
    }

    public static boolean isEnabled() {
        return enabled.get();
    }

    public static int getMinDelay() {
        return minDelay;
    }

    public static int getMaxDelay() {
        return maxDelay;
    }

    public static double getAverageLatency() {
        return latencyCount > 0 ? (double) totalLatencyApplied / latencyCount : 0;
    }

    public static void simulateLatency() {
        if (!enabled.get()) {
            return;
        }

        try {
            int delay = minDelay;
            if (maxDelay > minDelay) {
                delay += random.nextInt(maxDelay - minDelay);
            }

            synchronized (SimpleDbLatency.class) {
                totalLatencyApplied += delay;
                latencyCount++;
            }

            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}