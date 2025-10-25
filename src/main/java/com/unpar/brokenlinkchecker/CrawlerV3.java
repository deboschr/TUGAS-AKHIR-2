package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.Link;
import com.unpar.brokenlinkchecker.model.CheckingStatus;
import com.unpar.brokenlinkchecker.model.FetchResult;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javafx.application.Platform;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CrawlerV3 {
    private static final String USER_AGENT = "BrokenLinkChecker/1.0 (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)";
    private static final int TIMEOUT = 10000;
    private static final OkHttpClient OK_HTTP = new OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .build();

    // ==================== Callback (streaming data) ====================
    private final Consumer<CheckingStatus> checkingStatusConsumer;
    private final Consumer<Link> linkConsumer;

    // ==================== Internal state ====================
    // Untuk mengidentifikasi webpage
    private String rootHost;
    // Untuk menyimoan antrian webpage yang akan di crawling (FIFO/BFS)
    private final Queue<Link> frontier = new ArrayDeque<>();
    // Untuk menyimpan daftar unik setiap URL yang ditemukan
    private final Map<String, Link> repositories = new HashMap<>();

    private volatile boolean isRunning = false;

    public Crawler(Consumer<CheckingStatus> checkingStatusConsumer, Consumer<Link> linkConsumer) {
        this.checkingStatusConsumer = checkingStatusConsumer;
        this.linkConsumer = linkConsumer;
    }

    public void start(String seedUrl) {
        isRunning = true;

        // Notifikasi ke controller bahwa proses pemeriksaan dimulai
        send(checkingStatusConsumer, CheckingStatus.CHECKING);

        this.rootHost = getHostUrl(seedUrl);
        repositories.clear();
        frontier.clear();

        // Masukkan seed ke frontier
        frontier.offer(new Link(seedUrl));

        while (isRunning && !frontier.isEmpty()) {

            // Ambil link paling depan (FIFO)
            Link webpageLink = frontier.poll();

            // Masukan ke daftar unik dan skip kalau pernah dikunjungi
            if (!repositories.add(webpageLink.getUrl())) {
                continue;
            }

            // Kirim link ke controller
            send(allLinkConsumer, webpageLink.getUrl());

            // Fetch dan parse url dari webpage link
            FetchResult result = fetchUrl(webpageLink, true);

            // Ambil dokumen HTML
            Document doc = result.document();

            // Skip dan kirim broken link kalau error
            if (webpageLink.getStatusCode() >= 400 || webpageLink.getError() != "") {
                // Kirim link rusak ke controller
                send(brokenLinkConsumer, webpageLink);
                continue;
            }

            // Skip kalau beda host
            String finalUrlHost = getHostUrl(webpageLink.getFinalUrl());
            if (finalUrlHost == null || !finalUrlHost.equals(rootHost)) {
                continue;
            }

            // Skip kalau dokumen kosong atau bukan HTML
            if (doc == null || doc.selectFirst("html") == null) {
                continue;
            }

            // Kirim webpage link ke controller
            send(webpageLinkConsumer, webpageLink);

            // Ekstrak seluruh url yang ada di webpage
            Map<String, String> linksOnWebpage = extractUrl(doc);

            // Hapus koneksi WebpageLink dengan parentnya
            webpageLink.clearConnection();

            for (Map.Entry<String, String> entry : linksOnWebpage.entrySet()) {
                // Ambil url
                String entryUrl = entry.getKey();
                // Ambil anchor text
                String entryAnchorText = entry.getValue();
                // Ambil host
                String entryHost = getHostUrl(entryUrl);

                Link link = new Link(entry.getKey());

                // Kalau hostnya sama dengan seed url, maka masukan ke daftar yang akan di parse
                if (entryHost != null && entryHost.equals(rootHost)) {
                    // Set koneksi ke parentnya
                    link.setConnection(webpageLink, entryAnchorText);

                    // Masukan ke antrian
                    frontier.offer(link);
                }
                // Kalau tidak maka lansung kunjungi/cek tanpa parse
                else {
                    // Masukan ke daftar unik dan skip kalau pernah dikunjungi
                    if (!repositories.add(entryUrl)) {
                        continue;
                    }

                    // Kirim link ke controller
                    send(allLinkConsumer, entryUrl);

                    FetchResult entryRes = fetchUrl(link, false);

                    // Ambil link
                    Link entryLink = entryRes.link();

                    // Buat koneksi dengan parentnya dan kirim broken link
                    if (entryLink.getStatusCode() >= 400 || entryLink.getError() != null) {
                        entryLink.setConnection(webpageLink, entryAnchorText);
                        send(brokenLinkConsumer, entryLink);
                    }

                }

            }

        }

        // kalau keluar dari loop artinya sudah selesai
        if (isRunning) {
            send(checkingStatusConsumer, CheckingStatus.COMPLETED);
        }
    }

    public void stop() {
        isRunning = false;
        send(checkingStatusConsumer, CheckingStatus.STOPPED);
    }

    // =========================================================
    // Networking
    // =========================================================
    private FetchResult fetchUrl(Link link, Boolean isParseDoc) {
        try {
            Request request = new Request.Builder()
                    .url(link.getUrl())
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build();

            try (Response res = OK_HTTP.newCall(request).execute()) {

                Document doc = null;

                String contentType = res.header("Content-Type", "");
                boolean isHtml = contentType != null && contentType.toLowerCase().contains("text/html");

                if (isParseDoc && res.code() == 200 && isHtml && res.body() != null) {
                    try {
                        String html = res.body().string();

                        doc = Jsoup.parse(html, res.request().url().toString());
                    } catch (Exception parseErr) {
                        doc = null;
                    }
                }

                link.setFinalUrl(res.request().url().toString());
                link.setContentType(contentType);
                link.setStatusCode(res.code());

                return new FetchResult(link, doc);
            }

        } catch (Throwable e) {
            String errorName = e.getClass().getSimpleName();
            if (errorName == null || errorName.isBlank()) {
                errorName = "UnknownError";
            }

            link.setError(errorName);

            return new FetchResult(link, null);
        }
    }

    // =========================================================
    // Utility
    // =========================================================
    private Map<String, String> extractUrl(Document doc) {

        Map<String, String> results = new HashMap<>();

        for (Element a : doc.select("a[href]")) {

            String absoluteUrl = a.attr("abs:href");

            String cleanedUrl = normalizeUrl(absoluteUrl);

            if (cleanedUrl == null) {
                continue;
            }

            String anchorText = a.text().trim();

            if (!repositories.contains(cleanedUrl)) {
                results.put(cleanedUrl, anchorText);
            }
        }

        return results;
    }

    private String normalizeUrl(String rawUrl) {

        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return null;
        }

        URI url;
        try {
            url = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            return rawUrl;
        }

        // scheme
        String scheme = url.getScheme();
        if (scheme == null || scheme.isEmpty()) {
            return rawUrl;
        }
        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            return null;
        }
        scheme = scheme.toLowerCase();

        // host
        String host = url.getHost();
        if (host == null || host.isEmpty()) {
            return null;
        }

        // port
        int port = url.getPort();
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            port = -1;
        }

        // path
        String path = url.getPath();
        if (path == null) {
            path = "";
        }

        // query
        String query = url.getRawQuery();

        // Bangun ulang URL dengan objel URI tanpa menyertakan fragment
        try {
            URI cleanedUrl = new URI(scheme, null, host, port, path, query, null);
            return cleanedUrl.toASCIIString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private String getHostUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();

            if (host == null || host.isEmpty()) {
                return null;
            }

            // konversi ke format ASCII untuk domain internasional
            return IDN.toASCII(host.toLowerCase());
        } catch (IllegalArgumentException e) {
            // URL tidak valid secara sintaks
            return null;
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

    /**
     * Method ini gunanya buat ngambil objek Link dari "repositories" (map yang
     * nyimpen semua link unik), atau bikin baru kalau belum ada.
     *
     * Jadi: kalau URL-nya udah pernah ditemukan, dia balikin objek Link yang sama.
     * Tapi kalau belum pernah, dia langsung bikin Link baru, masukin ke map, dan
     * return objeknya.
     */
    private Link getOrCreateLink(String url) {
        /**
         * ******************************************
         * computeIfAbsent adalah versi lebih singkat dari:
         * 
         * if (!repositories.containsKey(url)) {
         * repositories.put(url, new Link(url))
         * }
         * return repositories.get(url)
         * 
         * ******************************************
         * Link::new disebut method reference di Java,
         * artinya sama kaya lambda expression.
         * 
         * repositories.computeIfAbsent(url, key -> new Link(key));
         */
        return repositories.computeIfAbsent(url, Link::new);
    }

    private <T> void send(Consumer<T> consumer, T data) {
        if (consumer != null) {
            Platform.runLater(() -> consumer.accept(data));
        }
    }
}