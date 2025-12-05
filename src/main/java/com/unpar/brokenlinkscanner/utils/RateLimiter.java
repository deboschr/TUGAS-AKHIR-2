package com.unpar.brokenlinkscanner.utils;

public class RateLimiter {
    // private final long INTERVAL = 1500L;
    private final long INTERVAL;

    private volatile long lastRequestTime = 0L;

    public RateLimiter(long interval) {
        this.INTERVAL = interval;
    }

    public synchronized void delay() {

        long now = System.currentTimeMillis();

        long waitTime = lastRequestTime + INTERVAL - now;

        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime = System.currentTimeMillis();
    }
}
