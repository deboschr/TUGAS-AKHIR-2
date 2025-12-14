package com.unpar.brokenlinkscanner.services;

import com.unpar.brokenlinkscanner.models.Link;
import com.unpar.brokenlinkscanner.utils.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Kelas Crawler bertugas melakukan proses crawling dan pemeriksaan tautan.
 *
 * Kelas ini mengimplementasikan algoritma dasar web crawler
 * dengan pendekatan/strategi breadth-first crawler (antrean FIFO).
 */
public class Crawler {
    // Untuk menyimpan daftar link internal (FIFO)
    private final Queue<Link> frontier = new ConcurrentLinkedQueue<>();

    // Untuk menyimpan daftar seluruh tautan yang telah diperiksa (unik)
    private final Map<String, Link> repositories = new ConcurrentHashMap<>();

    // Untuk menyimpan daftar rate limiter per host URL
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    // Penerima hasil crawling (MainController)
    private final LinkReceiver receiver;

    // Penanda apakah proses dihentikan oleh user
    private volatile boolean isStopped;

    // Executor untuk menjalankan pemeriksaan link secara paralel
    private ExecutorService executor;

    // Host dari URL awal untuk menentukan link internal atau eksternal
    private String rootHost;

    // Untuk melakukan HTTP request
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            // Mengikuti redirect
            .followRedirects(HttpClient.Redirect.ALWAYS)
            // Connection Timeout
            .connectTimeout(Duration.ofSeconds(20))
            // Bangun objek HttpClient
            .build();

    // Batas maksimal jumlah tautan yang diperiksa
    private static final int MAX_LINKS = 1000;

    /**
     * Receiver dikirim oleh MainController, sebagai penerima link hasil pemeriksaan.
     *
     * @param receiver : objek yang menerima hasil pemeriksaan link
     */
    public Crawler(LinkReceiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Method untuk menjalankan proses crawling.
     *
     * @param seedUrl : URL yang menjadi titik awal crawling
     */
    public void start(String seedUrl) {
        // Reset penanda pengentian jadi false
        isStopped = false;

        // Bersihkan data lama
        repositories.clear();
        rateLimiters.clear();
        frontier.clear();

        // Buat executor baru berbasis virtual thread
        executor = Executors.newVirtualThreadPerTaskExecutor();

        // Ambil host dari seed URL sebagai root host
        rootHost = UrlHandler.getHost(seedUrl);

        // Masukkan seed URL sebagai link pertama ke frontier
        frontier.offer(new Link(seedUrl));

        /**
         * Loop selama belum dihentikan user, frontier belum kosong dan jumlah total tautan belum melebihi batas
         */
        while (!isStopped && !frontier.isEmpty() && repositories.size() < MAX_LINKS) {
            // Ambil satu link halaman dari antrean paling depan
            Link webpageLink = frontier.poll();

            // Jika antrean kosong (race condition), hentikan
            if (webpageLink == null) {
                return;
            }

            // Periksa link internal dan ambil dokumen HTML jika memungkinkan
            Document html = checkLink(webpageLink, true);

            // Jika bukan webpage atau gagal ambil HTML, lanjut ke link berikutnya
            if (!webpageLink.isWebpage() || html == null) {
                continue;
            }

            // Ekstrak semua link dari halaman HTML
            Map<Link, String> linksOnWebpage = extractLink(html);

            // Daftar task paralel untuk link eksternal
            List<Callable<Void>> tasks = new ArrayList<>();

            // Iterasi setiap link yang ditemukan di halaman HTML
            for (var entry : linksOnWebpage.entrySet()) {
                // Jika user menghentikan proses, keluar
                if (isStopped) {
                    return;
                }

                // Ambil objek link
                Link link = entry.getKey();

                // Ambil teks anchor
                String anchorText = entry.getValue();

                // Cek apakah link sudah pernah diproses
                Link existingLink = repositories.get(link.getUrl());
                if (existingLink != null) {
                    // Jika sudah ada, tambahkan sumber halaman saja
                    existingLink.addWebpageSource(webpageLink, anchorText);
                    continue;
                } else {
                    // Jika belum ada, set sumber halaman pertama
                    link.addWebpageSource(webpageLink, anchorText);
                }

                if (UrlHandler.getHost(link.getUrl()).equalsIgnoreCase(rootHost)) {
                    // Jika link adalah link internal, maka masukkan ke frontier antrean paling belakang
                    frontier.offer(link);
                } else {
                    // Jika link adalah link eksternal, maka masukan ke tugas pararel
                    tasks.add(() -> {
                        // Jika proses dihentikan user, maka keluar dari metode
                        if (isStopped) return null;

                        // Periksa link eksternal tanpa mengambil halaman HTML
                        checkLink(link, false);

                        // Keluar dari metode
                        return null;
                    });
                }
            }

            if (!tasks.isEmpty()) {
                try {
                    // Jalankan semua task secara paralel jika ada
                    executor.invokeAll(tasks);
                } catch (InterruptedException e) {
                    // Restore interrupt status thread
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Method untuk menghentikan proses crawling.
     */
    public void stop() {
        // Set penanda jadi true
        isStopped = true;

        // Kosongkan antrean frontier
        frontier.clear();

        // Hentikan semua task yang sedang berjalan
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Method untuk memeriksa satu link menggunakan HTTP request.
     *
     * @param link       : objek Link yang akan diperiksa
     * @param isParseDoc : apakah response perlu diparse sebagai HTML
     * @return Document HTML jika berhasil, null jika tidak berhasil atau tidak dibutuhkan
     */
    private Document checkLink(Link link, boolean isParseDoc) {
        // Jika link sudah pernah diproses atau limit tercapai, hentikan
        if (repositories.get(link.getUrl()) != null || repositories.size() > MAX_LINKS) {
            return null;
        }

        try {
            // Ambil atau buat RateLimiter berdasarkan host URL
            RateLimiter limiter = rateLimiters.computeIfAbsent(UrlHandler.getHost(link.getUrl()), h -> new RateLimiter());
            // Terapkan delay sesuai rate limiter
            limiter.delay();

            // Membangun HTTP request
            HttpRequest req = HttpRequest.newBuilder()
                    // URL target
                    .uri(URI.create(link.getUrl()))
                    // Metode HTTP
                    .GET()
                    // Header User Agent
                    .header("User-Agent", "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2)")
                    // Request Timeout
                    .timeout(Duration.ofSeconds(20)).build();

            HttpResponse<?> res;
            if (isParseDoc) {
                // Jika parsing dilakukan maka butuh response body
                res = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            } else {
                // Jika parsing tidak dilakukan maka response body juga tidak dibutuhkan
                res = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.discarding());
            }

            // Update nilai atribut Final URL dari Link
            link.setFinalUrl(res.uri().toString());
            // Update nilai atribut Content Type dari Link
            link.setContentType(res.headers().firstValue("Content-Type").orElse("").toLowerCase());
            // Update nilai atribut Status Code dari Link
            link.setStatusCode(res.statusCode());

            Document html = null;
            boolean isFetchOk = link.getStatusCode() == 200 && res.body() != null;
            boolean isSameHost = UrlHandler.getHost(link.getFinalUrl()).equals(rootHost);

            if (isParseDoc && isFetchOk && isSameHost) {
                try {
                    // Ambil body response sebagai string
                    String body = (String) res.body();

                    // Parse HTML menggunakan Jsoup
                    html = Jsoup.parse(body, link.getFinalUrl());

                    // Tandai link sebagai webpage
                    link.setIsWebpage(true);
                } catch (Exception ignore) {
                    // Jika parsing gagal, abaikan
                    html = null;
                }
            }

            return html;
        } catch (Throwable e) {
            // Set pesan error berdasarkan exception yang terjadi
            link.setError(ErrorHandler.getExceptionError(e));
            // Kembalikan null jika gagal fetching/dll
            return null;
        } finally {
            // Masukkan link ke repository jika belum ada
            Link existing = repositories.putIfAbsent(link.getUrl(), link);

            // Jika ini link baru, kirim ke receiver (MainController)
            if (existing == null) {
                receiver.receive(link);
            }
        }
    }

    /**
     * Method untuk mengekstrak seluruh link dari dokumen HTML.
     *
     * @param html dokumen HTML hasil parsing
     * @return map Link ke anchor text
     */
    private Map<Link, String> extractLink(Document html) {
        // Map hasil ekstraksi link, pakai HashMap biar unik/tidak duplikat
        Map<Link, String> result = new HashMap<>();

        // Ambil semua elemen <a href>
        for (Element a : html.select("a[href]")) {
            // Ambil URL absolut dari atribut href
            String absoluteUrl = a.absUrl("href");

            // Jika URL kosong, skip
            if (absoluteUrl.isEmpty()) {
                continue;
            }

            // Normalisasi URL
            String normalizedUrl = UrlHandler.normalizeUrl(absoluteUrl, false);

            // Jika URL tidak valid, abaikan
            if (normalizedUrl == null) {
                continue;
            }

            // Simpan link beserta anchor text (tanpa duplikasi)
            result.putIfAbsent(new Link(normalizedUrl), a.text().trim());
        }

        return result;
    }

    /**
     * Method biar MainController tahu apakah proses crawling dihentikan user atau tidak.
     *
     * @return true jika dihentikan user
     */
    public boolean isStoppedByUser() {
        return isStopped;
    }
}
