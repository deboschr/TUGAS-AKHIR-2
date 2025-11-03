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

import com.unpar.brokenlinkchecker.utils.HttpHandler;
import com.unpar.brokenlinkchecker.utils.RateLimiter;
import com.unpar.brokenlinkchecker.utils.UrlHandler;
import javafx.application.Platform;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Crawler {
    // Untuk mengidentifikasi webpage
    private String rootHost;

    // Untuk menyimoan antrian URL webpage yang akan di crawling (FIFO/BFS)
    private final Queue<Link> frontier = new ArrayDeque<>();

    // Untuk menyimpan daftar unik setiap URL yang ditemukan
    private final Map<String, Link> repositories = new ConcurrentHashMap<>();

    // Untuk menyimpan rate limiter per host biar tiap host punya batas request-nya
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    // Untuk mengirim link yang ditemukan
    private final Consumer<Link> linkConsumer;

    // Untuk menandai status proses
    private volatile boolean isStopped = false;

    public Crawler(Consumer<Link> linkConsumer) {
        this.linkConsumer = linkConsumer;
    }

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
            Document doc = HttpHandler.fetch(currLink, true);

            // Kirim link ke controller
            send(currLink);

            if (!currLink.getError().isEmpty()) {
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
            Map<String, String> linksOnWebpage = extractLink(doc);

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
                        RateLimiter limiter = rateLimiters.computeIfAbsent(host, h -> new RateLimiter());
                        limiter.delay();

                        // Fetch URL tanpa parse, karena kita ga butuh doc
                        HttpHandler.fetch(link, false);

                        // Simpan ke repositories kalau belum ada
                        repositories.putIfAbsent(url, link);

                        // Kirim link ke controller
                        send(link);
                    }

                }));

                /**
                 * Tunggu semua task di halaman ini selesai sebelum lanjut ke frontier berikutnya
                 */
                executor.shutdown();
                executor.awaitTermination(15, TimeUnit.SECONDS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }

    public void stop() {
        isStopped = true;
    }

    private Map<String, String> extractLink(Document doc) {

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

    /**
     * Method ini bertugas buat ngirim objek link yang ditemukan selama proses
     * crawling ke MainController.
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

    public boolean isStopped() {
        return isStopped;
    }
}
