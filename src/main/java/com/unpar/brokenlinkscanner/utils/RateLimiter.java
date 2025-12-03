package com.unpar.brokenlinkscanner.utils;

public class RateLimiter {
    private static final long INTERVAL = 0L;

    private volatile long lastRequestTime = 0L;

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
