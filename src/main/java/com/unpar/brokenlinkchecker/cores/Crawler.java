package com.unpar.brokenlinkchecker.cores;

import com.unpar.brokenlinkchecker.models.Link;
import com.unpar.brokenlinkchecker.utils.RateLimiter;
import com.unpar.brokenlinkchecker.utils.UrlHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Crawler {
    // Untuk mengidentifikasi webpage (host yang sama dengan seed URL)
    private String rootHost;

    // Antrian webpage same-host yang akan di-crawling (FIFO / BFS)
    private final Queue<Link> frontier = new ConcurrentLinkedQueue<>();

    /*
     * Menyimpan semua URL unik yang sudah / akan dicek
     * Key : URL
     * Value : objek Link yang merepresentasikan URL tersebut
     */
    private final Map<String, Link> repositories = new ConcurrentHashMap<>();

    /*
     * Menyimpan rate limiter untuk memastikan setiap url yang di fetch dengan jarak
     * tertentu sesuai dengan hostnya.
     * Key : host URL
     * Value : ojek RateLimiter untuk membatasi kecepatan fetching
     */
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    // Callback ke controller buat kirim Link yang sudah dicek
    private final Consumer<Link> linkConsumer;

    // Flag kalau proses dihentikan oleh user
    private volatile boolean isStopped = false;

    // HttpClient bawaan Java, dipakai buat semua request HTTP
    private static final HttpClient HTTP_CLIENT = HttpClient
            // Bikin HttpClient dengan tanpa konfigurasi default
            .newBuilder()
            // Ikuti redirect dari server website
            .followRedirects(HttpClient.Redirect.ALWAYS)
            // Timeout saat bikin koneksi
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // Batas maksimum jumlah URL yang boleh dicek
    private static final int MAX_LINKS = 1000;

    public Crawler(Consumer<Link> linkConsumer) {
        this.linkConsumer = linkConsumer;
    }

    /**
     * Mulai proses crawling dari seedUrl dengan algoritma BFS.
     * - Hanya webpage same-host yang dimasukkan ke frontier dan di-crawl lebih
     * lanjut.
     * - Semua URL (internal maupun external) yang dicek akan dicatat di
     * repositories
     * sampai batas maksimum MAX_LINKS.
     *
     * @param seedUrl URL awal yang menjadi titik mulai crawling.
     */
    public void start(String seedUrl) {
        // Reset status stop (kalau sebelumnya pernah dihentikan user)
        isStopped = false;

        // Reset semua struktur data
        repositories.clear();
        frontier.clear();
        rateLimiters.clear();

        // Ambil host dari seed URL buat identifikasi same-host
        rootHost = UrlHandler.getHost(seedUrl);

        // Masukkan seed ke frontier sebagai titik awal BFS
        frontier.offer(new Link(seedUrl));

        // Loop BFS: selama user belum stop dan masih ada link (webpage link) di
        // frontier
        while (!isStopped && !frontier.isEmpty()) {

            // Kalau sudah mencapai limit global, hentikan BFS
            if (repositories.size() >= MAX_LINKS) {
                frontier.clear();
                break;
            }

            // Ambil link paling depan (FIFO)
            Link currLink = frontier.poll();
            if (currLink == null) {
                continue;
            }

            // Cek apakah URL ini sudah pernah dicatat di repositories
            Link existing = repositories.putIfAbsent(currLink.getUrl(), currLink);
            if (existing != null) {
                // Kalau sudah ada, berarti URL ini pernah (atau sedang) dicek maka skip
                continue;
            }

            // Fetch dan parse body dari webpage link (kalau HTML)
            Document doc = fetchLink(currLink, true);

            // Kirim hasil ke controller (apapun hasilnya: sukses / error)
            send(currLink);

            /*
             * Kalau ada error (exception, timeout, dll):
             * - currLink dianggap sebagai broken link
             * - Tidak diperlakukan sebagai webpage yang bisa di-crawling lagi.
             */
            if (!currLink.getError().isEmpty()) {
                continue;
            }

            /*
             * Skip kalau:
             * - doc == null (bukan HTML, misalnya PDF / image / dll)
             * - host dari finalUrl beda dengan host seedUrl (redirect ke domain lain)
             */
            String finalUrlHost = UrlHandler.getHost(currLink.getFinalUrl());
            if (doc == null || !finalUrlHost.equalsIgnoreCase(rootHost)) {
                continue;
            }

            // Kalau sampai sini, berarti currLink adalah webpage link yang valid
            currLink.setIsWebpage(true);

            // Ekstrak seluruh link dari webpage
            Map<Link, String> linksOnWebpage = extractLink(doc);

            // Executor berbasis virtual thread
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

                for (var entry : linksOnWebpage.entrySet()) {

                    // Kalau limit sudah tercapai, hentikan BFS dengan mengosongkan frontier
                    if (repositories.size() >= MAX_LINKS || isStopped) {
                        frontier.clear();
                        break;
                    }

                    Link link = entry.getKey();
                    String anchorText = entry.getValue();

                    // Cek apakah URL ini sudah pernah tercatat di repositories
                    Link existingLink = repositories.get(link.getUrl());
                    if (existingLink != null) {
                        // Kalau sudah ada, cukup tambahkan koneksi (source page + anchor text)
                        existingLink.addConnection(currLink, anchorText);
                        continue;
                    }

                    // URL ini belum pernah tercatat maka tambahkan koneksi pertama
                    link.addConnection(currLink, anchorText);

                    // Tentukan host-nya buat bedain same-host vs external
                    String host = UrlHandler.getHost(link.getUrl());

                    /**
                     * Kalau host sama dengan rootHost maka anggap sebagai webpage dan masukkan ke
                     * frontier
                     */
                    if (host.equalsIgnoreCase(rootHost)) {
                        /*
                         * Untuk webpage same-host:
                         * - Tidak langsung dimasukkan ke repositories di sini.
                         * - Link baru akan dianggap dicek dan dihitung ketika
                         * nanti diambil dari frontier dan di-fetch di loop BFS utama.
                         */
                        frontier.offer(link);
                    } else {
                        /*
                         * Untuk link non-same-host (external / beda host):
                         * - Dicek secara paralel di virtual thread.
                         * - Karena link ini akan benar-benar dicek (fetchLink),
                         * maka kita catat ke repositories
                         */

                        // Pastikan limit belum terlewati sebelum mencatat link baru
                        if (repositories.size() >= MAX_LINKS) {
                            frontier.clear();
                            break;
                        }

                        // Masukkan ke repositories sebagai link baru
                        repositories.putIfAbsent(link.getUrl(), link);

                        // Submit task ke virtual thread buat cek status link external
                        executor.submit(() -> {
                            /*
                             * Terapkan rate limiting per host biar ga dianggap serangan atau
                             * kena error HTTP 429 (Too Many Requests).
                             */
                            RateLimiter limiter = rateLimiters.computeIfAbsent(host, h -> new RateLimiter());
                            limiter.delay();

                            // Fetch URL tanpa parse HTML (kita cuma butuh status + header)
                            fetchLink(link, false);

                            // Kirim hasil ke controller
                            send(link);
                        });
                    }
                }

                // Tutup executor: tidak menerima task baru lagi
                executor.shutdown();

                // Tunggu sampai semua virtual thread selesai sebelum lanjut ke halaman frontier
                // berikutnya
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Hentikan proses crawling secara manual (oleh user).
     * Flag ini akan dicek di loop BFS dan di setiap task virtual thread.
     */
    public void stop() {
        isStopped = true;
    }

    /**
     * Method buat nge-fetch sebuah URL.
     * Bisa sekaligus parsing HTML kalau isParseDoc = true.
     *
     * @param link       objek Link yang akan di-update informasinya
     * @param isParseDoc true kalau body perlu di-parse jadi Document HTML
     * @return Document hasil parse HTML (kalau diminta dan valid), atau null kalau
     *         bukan HTML / error.
     */
    private Document fetchLink(Link link, boolean isParseDoc) {

        try {
            // variabel buat nyimpen hasil response dari server
            HttpResponse<?> res;

            /*
             * Kalau kita memang butuh parse HTML maka SELALU pakai GET. GET wajib dipakai
             * karena kita butuh isi body-nya (HTML)
             */
            if (isParseDoc) {
                HttpRequest request = HttpRequest.newBuilder()
                        // URL tujuan
                        .uri(URI.create(link.getUrl()))
                        // Header biar server tahu yg minta request aplikasi kita
                        .header("User-Agent",
                                "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)")
                        // Timeout total request (connect + read)
                        .timeout(Duration.ofSeconds(20))
                        // Pakai GET karena butuh body HTML lengkap
                        .GET()
                        // Build objek HttpRequest
                        .build();

                // Kirim request & baca seluruh body sebagai String
                res = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            }
            /*
             * Kalau TIDAK perlu parse maka coba HEAD dulu, pake HEAD karena lebih cepet
             * karena tidak download body
             */
            else {
                try {
                    HttpRequest headReq = HttpRequest.newBuilder()
                            // URL target
                            .uri(URI.create(link.getUrl()))
                            // Header biar server tahu yg minta request aplikasi kita
                            .header("User-Agent",
                                    "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)")
                            // Timeout total request (connect + read)
                            .timeout(Duration.ofSeconds(20))
                            // Method HEAD dg hanya minta header, tanpa body
                            .method("HEAD", HttpRequest.BodyPublishers.noBody())
                            // Build objek HttpRequest
                            .build();

                    // Kirim HEAD request, body dibuang (discard)
                    res = HTTP_CLIENT.send(headReq, HttpResponse.BodyHandlers.discarding());
                }
                /*
                 * Kalau HEAD gagal (server tidak support, SSL problem, dll) maka kita fallback
                 * ke GET, tapi tetap discard body biar cepat
                 */
                catch (Exception headError) {
                    HttpRequest getReq = HttpRequest.newBuilder()
                            .uri(URI.create(link.getUrl()))
                            .header("User-Agent",
                                    "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)")
                            .timeout(Duration.ofSeconds(20))
                            .GET()
                            .build();

                    // GET dipakai, tapi body langsung dibuang (nggak dibaca)
                    res = HTTP_CLIENT.send(getReq, HttpResponse.BodyHandlers.discarding());
                }
            }

            // Kode status HTTP (contoh 200, 404, 500, dll.)
            int statusCode = res.statusCode();
            // URL final setelah redirect (kalau ada)
            String finalUrl = res.uri().toString();
            // Ambil header Content-Type (bisa kosong kalau server tidak kirim)
            String contentType = res.headers().firstValue("Content-Type").orElse("").toLowerCase();

            Document doc = null;

            /*
             * Parse body response hanya kalau:
             * - isParseDoc = true
             * - status 200 OK
             * - header Content-Type mengandung text/html
             */
            if (isParseDoc && statusCode == 200 && contentType.contains("text/html")) {

                // body sudah String karena kita pakai BodyHandlers.ofString() di mode parse
                String body = (String) res.body();

                try {
                    // Jsoup parse body string ke dokumen HTML
                    doc = Jsoup.parse(body, finalUrl);
                } catch (Exception ignore) {
                    doc = null;
                }
            }

            link.setFinalUrl(finalUrl); // simpan final URL
            link.setContentType(contentType); // simpan tipe konten
            link.setStatusCode(statusCode); // set status (dan otomatis set error message)

            // bisa null kalau bukan HTML atau error
            return doc;

        } catch (Throwable e) {
            // Ambil nama error/class (misal "IOException")
            String errorName = e.getClass().getSimpleName();

            if (errorName.isBlank()) {
                errorName = "UnknownError";
            }

            // simpan nama error
            link.setError(errorName);

            return null;
        }
    }

    /**
     * Method ini bertugas buat ngambil semua tag a dengan atribut href.
     * dokumen HTML, terus kita convert ke URL absolut, normalisasi, dan simpan
     * sebagai objek Link.
     *
     * @param doc dokumen HTML
     * @return Map dengan key dan value:
     *         - key = objek Link (URL unik yang sudah dinormalisasi)
     *         - value = anchor text dari link tersebut di HTML ini
     */
    private Map<Link, String> extractLink(Document doc) {
        // Map hasil ekstraksi. Key: Link, Value: teks yang ada di dalam tag a
        Map<Link, String> result = new HashMap<>();

        // Loop semua elemen tag a yang punya atribut href
        for (Element a : doc.select("a[href]")) {

            // Ambil URL absolut dari atribut href (Jsoup akan gabungin dengan baseUri)
            String absoluteUrl = a.absUrl("href");

            // Skip kalau kosong, berarti ini bukan URL valid
            if (absoluteUrl.isEmpty()) {
                continue;
            }

            // Normalize URL biar konsisten (hapus fragment, lower-case host, dsb.)
            String normalizedUrl = UrlHandler.normalizeUrl(absoluteUrl);

            // Skip kalau gagal normalisasi
            if (normalizedUrl == null) {
                continue;
            }

            // Bikin objek Link baru berdasarkan URL yang udah bersih
            Link link = new Link(normalizedUrl);

            // Ambil teks yang ada di link
            String anchorText = a.text().trim();

            // Masukin ke map hanya kalau URL itu belum pernah tercatat sebelumnya
            result.putIfAbsent(link, anchorText);
        }

        // Balikin semua link yang berhasil diekstrak
        return result;
    }

    /**
     * Method ini bertugas buat ngirim objek Link yang ditemukan / dicek selama
     * proses crawling ke controller (MainController).
     *
     * @param link objek Link yang ditemukan / dicek selama proses crawling
     */
    private void send(Link link) {
        if (linkConsumer != null) {
            linkConsumer.accept(link);
        }
    }

    /**
     * Method getter biar controller bisa tahu apakah proses dihentikan oleh user
     * atau berhenti secara natural.
     *
     * @return true kalau dihentikan user, false kalau natural
     */
    public boolean isStopped() {
        return isStopped;
    }
}
