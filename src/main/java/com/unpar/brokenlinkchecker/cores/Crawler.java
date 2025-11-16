package com.unpar.brokenlinkchecker.cores;

import com.unpar.brokenlinkchecker.models.Link;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.unpar.brokenlinkchecker.utils.RateLimiter;
import com.unpar.brokenlinkchecker.utils.UrlHandler;
import javafx.application.Platform;
import org.jsoup.Jsoup;
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

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS) // Ikuti redirect dari server website
            .connectTimeout(Duration.ofSeconds(10)) // Batas waktu untuk membangun koneksi ke server
            .build();

    private static final int MAX_LINKS = 1000;

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

            if (repositories.size() >= MAX_LINKS) {
                break;
            } else if (repositories.putIfAbsent(currLink.getUrl(), currLink) != null) {
                /*
                 * Skip ke iterasi berikutnya kalau di repository sudah ada
                 * 
                 * putIfAbsent akan memasukan key & value jika key yang dimaksud belum ada
                 * dan akan mengembalikan null, namun kalau sudah ada key yang sama maka akan
                 * mengembalikan velue yang lama tapi value baru tidak akan dimasukan.
                 */
                continue;
            }

            // Fetch dan parse body dari webpage link
            Document doc = fetchLink(currLink, true);

            // Kirim link ke controller
            send(currLink);

            /*
             * Skip ke iterasi berikutnya kalau errornya ga kosong (terjadi error).
             * Karena kalau error berarti bukan webpage link tapi sebuah broken link.
             */
            if (!currLink.getError().isEmpty()) {
                continue;
            }

            /*
             * Skip kalau dokumen kosong (bukan HTML) atau kalau host dari finalUrl berbeda
             * dengan host dari seedUrl.
             * Pake finalUrl karena ada kemungkinan URL awal di redirect ke website lain
             * yang punya host yang berbeda.
             */
            String finalUrlHost = UrlHandler.getHost(currLink.getFinalUrl());
            if (doc == null || !finalUrlHost.equalsIgnoreCase(rootHost)) {
                continue;
            }

            // Tetapkan sebagai webpage
            currLink.setIsWebpage(true);

            // Ekstrak seluruh link yang ada di webpage
            Map<Link, String> linksOnWebpage = extractLink(doc);

            // Buat executor service berbasis virtual thread
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

                for (var entry : linksOnWebpage.entrySet()) {

                    // Hentikan iterasi bila limit tercapai
                    if (repositories.size() >= MAX_LINKS) {
                        frontier.clear(); // hentikan BFS
                        break; // hentikan loop ini
                    }

                    Link link = entry.getKey();
                    String anchorText = entry.getValue();

                    // Cek dulu apakah URL ini sudah ada di repositories
                    Link existingLink = repositories.get(link.getUrl());
                    if (existingLink != null) {
                        // Pake synchronized buat mencegah race condition pada objek existingLink
                        synchronized (existingLink) {
                            existingLink.addConnection(currLink, anchorText);
                        }
                        // Skip ke iterasi berikutnya
                        continue;
                    }


                    /*
                     * Bikin koneksi dengan webpage kalau ini link baru yang belum pernah ada di
                     * repository
                     */
                    link.addConnection(currLink, anchorText);

                    /*
                     * Untuk setiap link akan dieksekusi di thread yang berbeda.
                     * 
                     * Alasan pake virtual thread karena virtual thread adalah I/O bound, artinya
                     * tidak pakai CPU. Contohnya fetch link itu akan menunggu response.
                     */
                    executor.submit(() -> {

                        String host = UrlHandler.getHost(link.getUrl());

                        // Kalau hostnya sama dengan seed url, maka masukin ke daftar yang akan diparse
                        if (host.equalsIgnoreCase(rootHost)) {
                            if (repositories.size() < MAX_LINKS) {
                                // Masukan ke antrian paling belakang
                                frontier.offer(link);
                            }
                            return;
                        }

                        // Simpan ke repositories kalau belum ada
                        repositories.putIfAbsent(link.getUrl(), link);

                        /*
                         * Terapkan rate limiting perhost biar ga dianggap serangan atau dapat error 429
                         * (Too Many Requests).
                         * 
                         * computeIfAbsent bakal bikin objek kelas RateLimiter baru sebagai value kalau
                         * key nya (di sini host dari url) belum ada dan akan mengembalikan value yang
                         * baru. Tapi kalau key nya udah ada maka dia ga akan bikin velue baru dan Map
                         * ga akan berubah dan akan mengembalikan value yang udah ada.
                         */
                        RateLimiter limiter = rateLimiters.computeIfAbsent(host, h -> new RateLimiter());
                        limiter.delay();

                        // Fetch URL tanpa parse, karena kita ga butuh doc
                        fetchLink(link, false);

                        // Kirim link ke controller
                        send(link);

                    });
                }

                /*
                 * Setelah semua task link dari satu halaman disubmit ke executor,
                 * kita tutup executor biar ga menerima task baru lagi.
                 */
                executor.shutdown();

                /*
                 * Tunggu sampai semua virtual thread yang tadi disubmit selesai bekerja
                 * baru boleh lanjut ke frontier berikutnya. di sini kita pakai Long.MAX_VALUE
                 * biar timeout menunggunya lama banget dan itu artinya kita mau memastikan
                 * semua task selesai baru boleh lanjut.
                 * 
                 * ini bakal mengembalikan true kalau semua taks selesai sebelum timeout dan
                 * false kalau belum semua task selesai saat timeout.
                 */
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        isStopped = true;
    }

    private Document fetchLink(Link link, boolean isParseDoc) {



        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(link.getUrl()))
                    .header("User-Agent",
                            "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)")
                    .GET()
                    .build();

            // Kirim request dan ambil responsenya (body-nya langsung dalam bentuk String)
            HttpResponse<String> res = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = res.statusCode();
            String body = res.body();
            String finalUrl = res.uri().toString();

            // Content-Type bisa null kalau server tidak kirim header-nya
            String contentType = res.headers()
                    .firstValue("Content-Type")
                    .orElse("");

            Document doc = null;

            // Cek apakah kita perlu parsing HTML
            boolean isHtml = contentType.toLowerCase().contains("text/html");
            if (isParseDoc && statusCode == 200 && isHtml) {
                try {
                    doc = Jsoup.parse(body, finalUrl);
                } catch (Exception ignore) {
                    doc = null;
                }
            }

            link.setFinalUrl(finalUrl);
            link.setContentType(contentType);
            link.setStatusCode(statusCode);

            return doc;

        } catch (Throwable e) {
            String errorName = e.getClass().getSimpleName();
            if (errorName.isBlank()) {
                errorName = "UnknownError";
            }

            link.setError(errorName);

            return null;
        }
    }

    /**
     * Method ini bertugas buat ngambil semua <a href="..."> yang ada di dalam
     * dokumen HTML, terus kita convert ke URL absolut, normalisasi, dan simpan
     * sebagai objek Link.
     * 
     * @param doc dokumen HTML
     * @return Map<Link, String>:
     *         - key = objek Link
     *         - value = anchor text dari link tersebut di HTML ini
     */
    private Map<Link, String> extractLink(Document doc) {

        // Map hasil ekstraksi. Key: Link, Value: teks yang ada di dalam <a>...</a>
        Map<Link, String> result = new HashMap<>();

        // Loop semua elemen <a> yang punya atribut href
        for (Element a : doc.select("a[href]")) {

            // Ambil URL absolut dari atribut href (Jsoup akan gabungin dengan baseUri)
            String absoluteUrl = a.absUrl("href");

            // Skip kalau kosong, berarti ini bukan URL valid
            if (absoluteUrl.isEmpty()) {
                continue;
            }

            // Normalize URL biar konsisten
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
