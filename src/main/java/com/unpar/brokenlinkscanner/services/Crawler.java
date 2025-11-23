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
    /**
     * Untuk melakukan request HTTP
     *
     * - followRedirects => alway, biar nanti kita bisa mendapatkan final URL. Kalau
     * misalnya URL awal memiliki host yang sama dengan rootHost tapi final URL nya
     * memiliki host yang beda dengan rootHost maka ga perlu kita crawling, karna
     * itu artinya isi dia bukan merupakan halaman dari website yang lagi diperiksa.
     *
     * - connectTimeout => 5 detik, biar ga terlalu lama nunggu, kalau kelamaan
     * sistem jadi lambat, tapi kalau terlalu cepat juga bisa-bisa semua URL error
     * karena ga sempat bikin connection.
     */
    private static final HttpClient HTTP_CLIENT = HttpClient
            // Bikin HttpClient dengan tanpa konfigurasi default
            .newBuilder()
            // Ikuti redirect dari server website
            .followRedirects(HttpClient.Redirect.ALWAYS)
            // Timeout saat bikin koneksi
            .connectTimeout(Duration.ofSeconds(5)).build();

    /**
     * Untuk menyimpan request header user-agent, dipake biar server website tujuan
     * tahu siapa yang melakukan request, ini salah satu implementasi etika crawling
     */
    private static final String USER_AGENT = "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)";

    /**
     * Untuk ngebatasin jumlah link yang diperiksa, jadi ukuran dari repositories ga
     * boleh melebihi ini.
     * 1000 dipilih karena setelah beberapa kali percobaan, ketika sudah mencapat
     * 1000an, link aplikasi jadi lambat sekali, nyaris ga gerak.
     */
    private static final int MAX_LINKS = 1000;


    /**
     * Untuk menyimpan daftar antrian objek Link yang akan di-crawling.
     * Struktur data Queue dipakai karena cocok dengan skema FIFO pada algoritma BFS
     * (offer() menambah ke belakang, poll() mengambil dari depan).
     * 
     * ConcurrentLinkedQueue dipakai karena thread-safe.
     */
    private final Queue<Link> frontier = new ConcurrentLinkedQueue<>();

    /**
     * Untuk menyimpan semua URL unik yang sudah atau akan diperiksa selama proses
     * crawling. Digunakan untuk memastikan tidak ada URL yang diperiksa lebih dari
     * sekali.
     * 
     * Struktur data Map dipakai untuk menyimpan pasangan key dan value:
     * - Key : URL
     * - Value : Objek Link yang merepresentasikan URL tersebut
     * 
     * ConcurrentHashMap dipakai karena thread-safe.
     */
    private final Map<String, Link> repositories = new ConcurrentHashMap<>();

    /**
     * Untuk menyimpan objek RateLimiter per host URL. Digunakan untuk memastikan
     * pembatasan kecepatan fetching dilakukan per-host URL agar tidak dianggap
     * sebagai serangan oleh server website tujuan dan tidak mendapat error 429
     * (Too Many Requests).
     * 
     * Struktur data Map dipakai untuk menyimpan pasangan key dan value:
     * - Key : Host dari URL
     * - Value : Objek RateLimiter untuk membatasi fetching ke host tersebut
     * 
     * ConcurrentHashMap dipakai karena thread-safe.
     */
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    /**
     * Fungsi callback buat ngirim objek Link yang udah di fetching kembali ke
     * controller.
     *
     * Pake function interface Consumer karena dia cuma punya 1 method (accept), dan ga mengembalikan apa-apa.
     */
    private final Consumer<Link> linkConsumer;

    /*
     * Untuk menyimpan host dari seed URL.
     * Digunakan untuk membandingkan host dari URL lain, kalau sama maka berpotensi
     * menjadi URL halaman situs web (Webpage Link).
     */
    private String rootHost;

    /**
     * Penanda buat nentuin apakah proses dihentikan user atau tidak.
     * True: Kalau proses dihentikan user
     * False: Kalau proses tidak dihentikan
     */
    private volatile boolean isStopped = false;

    public Crawler(Consumer<Link> linkConsumer) {
        this.linkConsumer = linkConsumer;
    }

    /**
     * Method untuk menjalankan proses crawling.
     *
     * @param seedUrl URL yang menjadi titik awal proses crawling
     */
    public void start(String seedUrl) {
        // Reset semua data
        isStopped = false;
        repositories.clear();
        rateLimiters.clear();
        frontier.clear();

        // Ambil host dari seed URL buat nanti identifikasi webpage link
        rootHost = URLHandler.getHost(seedUrl);

        // Buat objek link baru + masukin ke fontier urutan paling belakang
        frontier.offer(new Link(seedUrl));

        /**
         * Loop untuk crawling, mengimplementasikan algoritma BFS.
         * Jadi kita ga akan berpindah ke fontier berikutnya kalau fontier yang saat ini
         * belum selesai diperiksa semua, meskipun kita pakai banyak thread.
         * 
         * Loop akan tetap berjalan selama:
         * - tidak dihentikan user
         * - frontier masih ada isinya
         * - link yang diperiksa belum melebihi 1000
         */
        while (!isStopped && !frontier.isEmpty() && repositories.size() < MAX_LINKS) {

            // Ambil link antrian paling depan
            Link currLink = frontier.poll();

            /**
             * Kalau dapet null, misalnya frontier tiba-tiba kosong, langsung lanjut ke
             * iterasi berikutnya. ini buat antisipasi aja.
             */
            if (currLink == null) {
                continue;
            }

            /**
             * Cek apakah link ini sudah pernah dicatat di repositories, kalau sudah ada,
             * berarti link ini udah pernah di periksa, jadi ga perlu di periksa lagi.
             */
            Link existing = repositories.putIfAbsent(currLink.getUrl(), currLink);
            if (existing != null) {
                continue;
            }

            /**
             * Fetach dan kirim perintah buat nge-parse response-bodynya ke dokumen HTML.
             * Kita kirim perintah buat nge-parse karna link ini diambil dari frontier, jadi
             * berpotensi jadi halaman website, kalau halaman website maka kita akan ekstrak
             * link didalamnya.
             */
            Document doc = fetchLink(currLink, true);

            // Kirim hasil ke controller apapun hasilnya, sukses / error
            send(currLink);

            /**
             * Skip kalau:
             * - error, berarti Broken link
             * - doc adalah null, berarti bukan HTML (ga bisa ekstrak link)
             * - host dari finalUrl beda dengan rootHost (redirect ke domain lain)
             */
            String finalUrlHost = URLHandler.getHost(currLink.getFinalUrl());
            if (!currLink.getError().isEmpty() || doc == null || !finalUrlHost.equalsIgnoreCase(rootHost)) {
                continue;
            }

            // Kalau sampai sini, berarti link saat ini adalah webpage link yang valid
            currLink.setIsWebpage(true);

            // Ekstrak seluruh link dari webpage
            Map<Link, String> linksOnWebpage = extractLink(doc);

            /**
             * Bikin executor buat bikin virtual thread per task.
             * Executor ini bertugas buat mengelola virtual thread, jadi kita ga perlu bikin
             * satu persatu thread, cukup kita bikin tugas, terus tugas itu kita submit ke
             * executor, nanti executor yang bakal bikinin thread buat tugas itu. Mirip kaya
             * thread pool, jadi kita punya kontrol terpusat, ini penting karna kita mau
             * mastiin thread yang kita buat di sini, benar2 selesai dulu semuanya sebelum
             * lanjut ke iterasi berikutnya.
             */
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

                // Loop semua link yang berhasil diekstrak dari webpage
                for (var entry : linksOnWebpage.entrySet()) {
                    /**
                     * Kalau jumlah link udah melebihi batas atau user mengentikan proses, maka kita
                     * keluar dari looping ini dan pastikan frontier kita kosongin juga biar ga bisa
                     * crawling lagi.
                     */
                    if (repositories.size() >= MAX_LINKS || isStopped) {
                        frontier.clear();
                        break;
                    }

                    Link link = entry.getKey();
                    String anchorText = entry.getValue();

                    /**
                     * Cek apakah link ini sudah pernah dicatat di repositories, kalau sudah ada,
                     * berarti link ini udah pernah di periksa, jadi ga perlu di periksa lagi, tapi
                     * kita tambahin koneksinya ke webpage ini biar ketahuan bahwa link ini pernah
                     * ditemukan di webpage ini juga.
                     */
                    Link existingLink = repositories.get(link.getUrl());
                    if (existingLink != null) {
                        existingLink.addConnection(currLink, anchorText);
                        continue;
                    }

                    // Kalau belum pernah di periksa, berarti kita bikin koneksi pertama
                    link.addConnection(currLink, anchorText);

                    // Ambil host dari URL link ini
                    String host = URLHandler.getHost(link.getUrl());

                    /**
                     * Kalau hostnya sama dengan rootHost berarti link ini berpotensi jadi webpage.
                     * maka kita masukan ke frontier antrian paling belakang.
                     */
                    if (host.equalsIgnoreCase(rootHost)) {
                        frontier.offer(link);
                    }
                    /**
                     * Kalau hostnya beda dengan rootHost berarti link ini adalah link eksternal, ga
                     * perlu di crawling cukup di fetch aja.
                     */
                    else {
                        // Masukin ke repository kalau belum ada
                        repositories.putIfAbsent(link.getUrl(), link);

                        /**
                         * Disini kita bakal submit tugas ke dalam executor dan executor bakal bikin
                         * virtual thread tersendiri untuk masing-masing tugas yang kita submit.
                         * Tugas di sini I/O bound jadi cocok buat virtual thread.
                         */
                        executor.submit(() -> {
                            // Ambil atau buat RateLimiter untuk host ini
                            RateLimiter limiter = rateLimiters.computeIfAbsent(host, h -> new RateLimiter());
                            // Kasih delay ke thread ini buat ngelakuin request
                            limiter.delay();

                            /**
                             * Fetch link ini pake thread tersendiri, tapi jangan minta buat nge-parse
                             * response body karena ini URL eksternal.
                             */
                            fetchLink(link, false);

                            // Kirim link yang atributnya udah di update saat fetching
                            send(link);
                        });
                    }
                }

                // Kasih tau executor kalau kita ga akan submit task lagi
                executor.shutdown();

                /**
                 * Tunggu semua virtual thread selesai ngerjain tugasnya, baru kita boleh lanjut
                 * ke frontier berikutnya. Disini kita pake long max value biar semua thread
                 * benar-benar ditunggu sampai selesai.
                 */
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Method untuk mengantikan proses crawling oleh user
     */
    public void stop() {
        isStopped = true;
        frontier.clear();
    }

    /**
     * Method buat fetching URL dan parsing response body kalau diminta
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
     *         - key = objek Link (URL unik yang sudah dinormalisasi)
     *         - value = anchor text dari link tersebut di HTML ini
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

            // Normalize URL
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
