package com.unpar.brokenlinkscanner.utils;

public class RateLimiter {
    // Waktu jarak antar request
    private static final long INTERVAL = 1800L;

    // Waktu dari request terakhir
    private volatile long lastRequestTime = 0L;

    /**
     * Method utama untuk mengatur delay atau jarak waktu antar satu request ke
     * request yang lain.
     *
     * synchronized artinya cuma satu thread dalam satu waktu yang bisa menjalankan
     * method ini.
     */
    public synchronized void delay() {
        // Waktu saat ini dalam epoch
        long now = System.currentTimeMillis();

        // Waktu untuk menggu
        long waitTime = lastRequestTime + INTERVAL - now;

        // Kalau masih belum lewat 500 ms dari request terakhir, tunggu dulu
        if (waitTime > 0) {
            try {
                // Hentikan thread sementara, selama nilai waitTime
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                // Kalau thread dibatalkan, set flag interrupted biar caller tahu
                Thread.currentThread().interrupt();
            }
        }

        // update waktu terakhir dengan waktu sekarang
        lastRequestTime = System.currentTimeMillis();
    }
}
