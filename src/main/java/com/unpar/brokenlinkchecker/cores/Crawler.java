package com.unpar.brokenlinkchecker.cores;

import com.unpar.brokenlinkchecker.models.Link;


import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.unpar.brokenlinkchecker.utils.UrlHandler;
import javafx.application.Platform;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Crawler {
    // ===================== Konstanta =====================
    private static final String USER_AGENT = "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)";
    private static final OkHttpClient OK_HTTP = new OkHttpClient.Builder().followRedirects(true).connectTimeout(10, TimeUnit.SECONDS) // Batas waktu untuk membangun koneksi ke server
            .readTimeout(20, TimeUnit.SECONDS) // Batas waktu untuk membaca respons dari server
            .build();

    // ==================== Internal state ====================
    // Untuk mengidentifikasi webpage
    private String rootHost;

    // Untuk menyimoan antrian URL webpage yang akan di crawling (FIFO/BFS)
    private final Queue<Link> frontier = new ArrayDeque<>();

    // Untuk menyimpan daftar unik setiap URL yang ditemukan
    private final Map<String, Link> repositories = new ConcurrentHashMap<>();

    // Untuk menyimpan rate limiter per host biar tiap host punya batas request-nya
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    // Untuk menandai status proses
    private volatile boolean isStopped = false;

    // ===================== Callback =====================

    // Untuk mengirim link yang ditemukan
    private final Consumer<Link> linkConsumer;

    public Crawler(Consumer<Link> linkConsumer) {
        this.linkConsumer = linkConsumer;
    }

    // =========================================================
    // Core
    // =========================================================

    public void start(String seedUrl) {
        // set status
        isStopped = false;

        // reset penyimpanan
        repositories.clear();
        frontier.clear();
        rateLimiters.clear();

        // set init value
        rootHost = UrlHandler.getHost(seedUrl);
        frontier.offer(new Link(seedUrl));

        while (!isStopped && !frontier.isEmpty()) {
            // Ambil link paling depan (FIFO)
            Link currLink = frontier.poll();

            if (repositories.putIfAbsent(currLink.getUrl(), currLink) != null) {
                continue;
            }

            // Fetch dan parse body dari webpage link
            Document doc = fetchUrl(currLink, true);

            // Kirim link ke controller
            send(currLink);

            if (currLink.getError().isEmpty()) {
                continue;
            }

            // Skip kalau dokumen kosong (bukan HTML) atau kalau beda host
            String finalUrlHost = UrlHandler.getHost(currLink.getFinalUrl());
            if (doc == null || !finalUrlHost.equalsIgnoreCase(rootHost)) {
                continue;
            }

            // Tetapkan sebagai webpage
            currLink.setIsWebpage(true);

            // Ekstrak seluruh url yang ada di webpage
            Map<String, String> linksOnWebpage = extractUrl(doc);

            // Jalankan pemrosesan tiap link di virtual thread terpisah
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

                linksOnWebpage.forEach((url, anchorText) -> executor.submit(() -> {

                    // Cek dulu apakah URL ini sudah ada di repositories
                    Link existingLink = repositories.get(url);
                    if (existingLink != null) {
                        // Pake synchronized untuk mencegah race condition pada objek existingLink
                        synchronized (existingLink) {
                            existingLink.addConnection(currLink, anchorText);
                        }
                        // Skip ke iterasi berikutnya
                        return;
                    }

                    // Ambil hostnya buat dibandingan sama host seed url
                    String host = UrlHandler.getHost(url);

                    // Buat objek link baru dan bikin koneksi dengan webpage
                    Link link = new Link(url);
                    link.addConnection(currLink, anchorText);

                    // Kalau hostnya sama dengan seed url, maka masukan ke daftar yang akan di parse
                    if (host.equalsIgnoreCase(rootHost)) {
                        // Masukan ke antrian paling belakang
                        frontier.offer(link);
                    }
                    // Kalau tidak maka lansung kunjungi/cek
                    else {
                        // Fetch URL tanpa parse, karena kita ga butuh doc
                        fetchUrl(link, false);

                        // Simpan ke repositories kalau belum ada
                        repositories.putIfAbsent(url, link);

                        // Kirim link ke controller
                        send(link);
                    }

                }));

                // Tunggu semua task di halaman ini selesai sebelum lanjut ke frontier
                // berikutnya
                executor.shutdown();
                executor.awaitTermination(30, TimeUnit.SECONDS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }

    public void stop() {
        isStopped = true;
    }

    private Document fetchUrl(Link link, Boolean isParseDoc) {
        try {
            String host = UrlHandler.getHost(link.getUrl());
            RateLimiter limiter = rateLimiters.computeIfAbsent(host, h -> new RateLimiter());
            limiter.delay();

            Request request = new Request.Builder().url(link.getUrl()).header("User-Agent", USER_AGENT).get().build();

            try (Response res = OK_HTTP.newCall(request).execute()) {

                Document doc = null;
                int statusCode = res.code();
                String contentType = res.header("Content-Type", "");
                String finalUrl = res.request().url().toString();

                boolean isHtml = contentType.toLowerCase().contains("text/html");
                if (isParseDoc && statusCode == 200 && isHtml) {
                    try {
                        String html = res.body().string();

                        doc = Jsoup.parse(html, finalUrl);
                    } catch (Exception parseErr) {
                        doc = null;
                    }
                }

                link.setFinalUrl(finalUrl);
                link.setContentType(contentType);
                link.setStatusCode(statusCode);

                return doc;
            }

        } catch (Throwable e) {
            String errorName = e.getClass().getSimpleName();
            if (errorName.isBlank()) {
                errorName = "UnknownError";
            }

            link.setError(errorName);

            return null;
        }
    }

    private Map<String, String> extractUrl(Document doc) {

        Map<String, String> urlMap = new HashMap<>();

        for (Element a : doc.select("a[href]")) {

            String absoluteUrl = a.attr("abs:href");

            String normalizedUrl = UrlHandler.normalizeUrl(absoluteUrl);

            if (normalizedUrl != null) {
                urlMap.putIfAbsent(normalizedUrl, a.text().trim());
            }
        }

        return urlMap;
    }

    // =========================================================
    // Utility
    // =========================================================

    /**
     * Method ini bertugas buat ngirim objek link yang ditemukan selama proses
     * crawling ke MainController.
     *
     * Proses crawling dijalankan di background thread, sedangkan JavaFX cuma boleh
     * update komponen GUI dari thread utamanya (JavaFX Application Thread). Jadi
     * biar gak error, kita bungkus pemanggilan consumer pakai Platform.runLater(),
     * supaya dijalankan di thread UI dengan aman.
     *
     * @param link objek link yang ditemukan selama proses crawling
     */
    private void send(Link link) {
        if (linkConsumer != null) {
            Platform.runLater(() -> linkConsumer.accept(link));
        }
    }

    private static class RateLimiter {
        // Waktu jarak antar request, karena 500ms maka hanya 2 req per detik
        private static final long INTERVAL = 500L;

        // Waktu dari request terakhir
        private volatile long lastRequestTime = 0L;

        /**
         * Method utama untuk mengatur delay atau jarak waktu antar satu request ke
         * request yang lain.
         *
         * synchronized artinya cuma satu thread dalam satu waktu yang bisa menjalankan
         * method ini untuk 1 host yang sama. Jadi kalau ada 3 thread yang akses host
         * yang sama barengan, mereka akan ngantri.
         */
        public synchronized void delay() {
            // Waktu saat ini dalam epoch
            long now = System.currentTimeMillis();

            // Waktu untuk menggu
            long waitTime = lastRequestTime + INTERVAL - now;

            // Kalau masih belum lewat 500 ms dari request terakhir, tunggu dulu
            if (waitTime > 0) {
                try {
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

    public boolean isStopped() {
        return isStopped;
    }
}
