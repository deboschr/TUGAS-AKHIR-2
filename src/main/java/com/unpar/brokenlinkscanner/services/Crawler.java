package com.unpar.brokenlinkscanner.services;

import com.unpar.brokenlinkscanner.models.Link;
import com.unpar.brokenlinkscanner.utils.RateLimiter;
import com.unpar.brokenlinkscanner.utils.URLHandler;
import javafx.application.Platform;
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
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Crawler {
    // Untuk mengidentifikasi webpage (host yang sama dengan seed URL)
    private String rootHost;

    // Untuk menyimpan atrean link yang akan di-crawling (FIFO / BFS)
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
            .connectTimeout(Duration.ofSeconds(5)).build();

    private static final String USER_AGENT = "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)";

    // Batas maksimum jumlah URL yang boleh dicek
    private static final int MAX_LINKS = 1000;

    /**
     * Constraktor dari kelas Crawler.
     * Pake Consumer karena consumer adalah function interface java yang menerima
     * satu buah input (kalau di sini objek kelas Link), lalu memproses input itu
     * sesuai dengan logika fungsi yang didefinisikan di kelas yang membuat instance
     * dari kelas Crawler ini, lalu tidak memberikan return apa2.
     *
     * @param linkConsumer fungsi buat mengirim data Link yang ditemukan.
     */
    public Crawler(Consumer<Link> linkConsumer) {
        this.linkConsumer = linkConsumer;
    }

    /**
     * Method untuk menjalankan proses crawling.
     *
     * @param seedUrl URL yang menjadi titik awal proses crawling
     */
    public void start(String seedUrl) {
        // Reset status stop (kalau sebelumnya pernah dihentikan user)
        isStopped = false;

        // Reset semua struktur data
        repositories.clear();
        frontier.clear();
        rateLimiters.clear();

        // Ambil host dari seed URL buat identifikasi webpage
        rootHost = URLHandler.getHost(seedUrl);

        // Masukkan seed ke frontier sebagai titik awal BFS
        frontier.offer(new Link(seedUrl));

        // Loop BFS: selama user belum stop dan masih ada link (webpage link) di
        // frontier
        while (!isStopped && !frontier.isEmpty() && repositories.size() < MAX_LINKS) {

            // Ambil link paling depan (FIFO)
            Link currLink = frontier.poll();
            if (currLink == null) {
                continue;
            }

            // Cek apakah URL ini sudah pernah dicatat di repositories
            Link existing = repositories.putIfAbsent(currLink.getUrl(), currLink);
            if (existing != null) {
                // Kalau sudah ada, berarti URL ini pernah dicek maka skip
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
            String finalUrlHost = URLHandler.getHost(currLink.getFinalUrl());
            if (doc == null || !finalUrlHost.equalsIgnoreCase(rootHost)) {
                continue;
            }

            // Kalau sampai sini, berarti currLink adalah webpage link yang valid
            currLink.setIsWebpage(true);

            // Ekstrak seluruh link dari webpage
            Map<Link, String> linksOnWebpage = extractLink(doc);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (var entry : linksOnWebpage.entrySet()) {
                    if (repositories.size() >= MAX_LINKS || isStopped) {
                        frontier.clear();
                        break;
                    }

                    Link link = entry.getKey();
                    String anchorText = entry.getValue();

                    Link existingLink = repositories.get(link.getUrl());
                    if (existingLink != null) {
                        existingLink.addConnection(currLink, anchorText);
                        continue;
                    }

                    link.addConnection(currLink, anchorText);

                    String host = URLHandler.getHost(link.getUrl());
                    if (host.equalsIgnoreCase(rootHost)) {
                        frontier.offer(link);
                    } else {
                        repositories.putIfAbsent(link.getUrl(), link);

                        executor.submit(() -> {
                            RateLimiter limiter = rateLimiters.computeIfAbsent(host, h -> new RateLimiter());
                            limiter.delay();

                            fetchLink(link, false);

                            send(link);
                        });
                    }
                }

                executor.shutdown();

                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Method untuk mengantikan proses crawling (oleh user)
     */
    public void stop() {
        isStopped = true;
    }

    /**
     * Method buat fetching URL dan parsing response body kalau diminta
     *
     * @param link       objek Link yang akan di-update informasinya
     * @param isParseDoc true kalau body perlu di-parse jadi Document HTML
     * @return Document hasil parse HTML (kalau diminta dan valid), atau null kalau
     * bukan HTML / error.
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
                        // Header biar server tahu yg minta request adalah aplikasi kita
                        .header("User-Agent", USER_AGENT)
                        // Timeout total request (connect + read)
                        .timeout(Duration.ofSeconds(10))
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
                            // Header biar server tahu yg minta request adalah aplikasi kita
                            .header("User-Agent", USER_AGENT)
                            // Timeout total request (connect + read)
                            .timeout(Duration.ofSeconds(10))
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
                 */ catch (Exception headError) {
                    HttpRequest getReq = HttpRequest
                            .newBuilder()
                            // URL target
                            .uri(URI.create(link.getUrl()))
                            // Header biar server tahu yg minta request adalah aplikasi kita
                            .header("User-Agent", USER_AGENT)
                            // Timeout total request (connect + read)
                            .timeout(Duration.ofSeconds(10))
                            // Method GET
                            .GET()
                            // Build objek HttpRequest
                            .build();

                    // GET dipakai, tapi body langsung dibuang (nggak dibaca)
                    res = HTTP_CLIENT.send(getReq, HttpResponse.BodyHandlers.discarding());
                }
            }

            // Kode status HTTP
            int statusCode = res.statusCode();
            // URL final setelah redirect (kalau ada)
            String finalUrl = res.uri().toString();
            // Ambil header Content-Type (bisa kosong kalau server tidak kirim)
            String contentType = res.headers().firstValue("Content-Type").orElse("").toLowerCase();

            Document doc = null;

            /*
             * Parse body response hanya kalau:
             * - diminta untuk parsing
             * - request-nya oke
             * - response body-nya HTML
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
     * Method buat mengambil semua link dari suatu dokumen HTML.
     * URL akan di ekstrak dari tag a HTML pada atribut href
     *
     * @param HTML dokumen HTML
     * @return Map dengan key dan value:
     * - key = objek Link (URL unik yang sudah dinormalisasi)
     * - value = anchor text dari link tersebut di HTML ini
     */
    private Map<Link, String> extractLink(Document HTML) {
        // Map hasil ekstraksi. Key: Link, Value: teks yang ada di dalam tag a
        Map<Link, String> result = new HashMap<>();

        // Loop semua elemen tag a yang punya atribut href
        for (Element a : HTML.select("a[href]")) {

            // Ambil URL absolut dari atribut href (Jsoup akan gabungin dengan baseUri)
            String absoluteUrl = a.absUrl("href");

            // Skip kalau kosong, berarti ini bukan URL valid
            if (absoluteUrl.isEmpty()) {
                continue;
            }

            // Normalize URL biar konsisten (hapus fragment, lower-case host, dsb.)
            String normalizedUrl = URLHandler.normalizeUrl(absoluteUrl);

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
     * Disini pake Platform.runLater (dari javafx) biar data yang dikirim bisa
     * diterima oleh controller, karena controller dijalankan pake thread javafx
     * sedangkan crawler thread yang berbeda.
     *
     * @param link objek Link yang ditemukan crawling
     */
    private void send(Link link) {
        if (linkConsumer != null) {
            Platform.runLater(() -> linkConsumer.accept(link));
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
