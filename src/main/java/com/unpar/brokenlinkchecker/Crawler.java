package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.Link;
import com.unpar.brokenlinkchecker.model.CheckingStatus;
import com.unpar.brokenlinkchecker.model.FetchResult;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
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

public class Crawler {
    private static final String USER_AGENT = "BrokenLinkChecker/1.0 (+https://github.com/jakeschr/broken-link-checker; contact: 6182001060@student.unpar.ac.id)";
    private static final int TIMEOUT = 10000;
    private static final OkHttpClient OK_HTTP = new OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .build();

    // ==================== Callback (streaming data) ====================
    private final Consumer<CheckingStatus> statusConsumer;
    private final Consumer<String> totalLinkConsumer;
    private final Consumer<Link> webpageLinkConsumer;
    private final Consumer<Link> brokenLinkConsumer;

    // ==================== Internal state ====================
    private String rootHost;
    private final Queue<Link> frontier = new ArrayDeque<>();
    private final Set<String> repositories = new HashSet<>();

    private volatile boolean isRunning = false;

    // ==================== Constructor ====================
    public Crawler(Consumer<CheckingStatus> statusConsumer,
            Consumer<String> totalLinkConsumer,
            Consumer<Link> webpageLinkConsumer,
            Consumer<Link> brokenLinkConsumer) {
        this.statusConsumer = statusConsumer;
        this.totalLinkConsumer = totalLinkConsumer;
        this.webpageLinkConsumer = webpageLinkConsumer;
        this.brokenLinkConsumer = brokenLinkConsumer;
    }

    // =========================================================
    // Main
    // =========================================================
    public void start(String seedUrl) {
        isRunning = true;

        // Notifikasi mulai
        sendStatus(CheckingStatus.CHECKING);

        this.rootHost = getHostUrl(seedUrl);
        repositories.clear();
        frontier.clear();

        // masukkan seed ke frontier
        frontier.offer(new Link(seedUrl, null, 0, null, null, Instant.now()));

        while (isRunning && !frontier.isEmpty()) {

            // Ambil link paling depan (FIFO)
            Link link = frontier.poll();

            // Masukan ke daftar unik dan skip kalau pernah dikunjungi
            if (!repositories.add(link.getUrl())) {
                continue;
            }

            // Kirim total link
            sendTotalLink(link.getUrl());

            // Fetch dan parse url dari webpage link
            FetchResult result = fetchUrl(link.getUrl(), true);

            // Ambil webpage link
            Link webpageLink = result.link();
            // Ambil dokumen HTML
            Document doc = result.document();

            // Skip dan kirim broken link kalau error
            if (webpageLink.getStatusCode() >= 400 || webpageLink.getError() != null) {
                sendBrokenLink(webpageLink);
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

            // Kirim webpage link
            sendWebpageLink(webpageLink);

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

                // Kalau hostnya sama dengan seed url, maka masukan ke daftar yang akan di parse
                if (entryHost != null && entryHost.equals(rootHost)) {
                    Link entryLink = new Link(entryUrl, null, 0, null, null, Instant.now());

                    // Set koneksi ke parentnya
                    entryLink.setConnection(webpageLink, entryAnchorText);

                    // Masukan ke antrian
                    frontier.offer(entryLink);
                }
                // Kalau tidak maka lansung kunjungi/cek tanpa parse
                else {
                    // Masukan ke daftar unik dan skip kalau pernah dikunjungi
                    if (!repositories.add(entryUrl)) {
                        continue;
                    }

                    // Kirim total link
                    sendTotalLink(entryUrl);

                    FetchResult entryRes = fetchUrl(entryUrl, false);

                    // Ambil link
                    Link entryLink = entryRes.link();

                    // Buat koneksi dengan parentnya dan kirim broken link
                    if (entryLink.getStatusCode() >= 400 || entryLink.getError() != null) {
                        entryLink.setConnection(webpageLink, entryAnchorText);
                        sendBrokenLink(entryLink);
                    }

                }

            }

        }

        // kalau keluar dari loop artinya sudah selesai
        if (isRunning) {
            sendStatus(CheckingStatus.COMPLETED);
        }
    }

    public void stop() {
        isRunning = false;
        sendStatus(CheckingStatus.STOPPED);
    }

    // =========================================================
    // Networking
    // =========================================================
    private FetchResult fetchUrl(String url, Boolean isParseDoc) {
        try {
            Request request = new Request.Builder()
                    .url(url)
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

                Link link = new Link(url, res.request().url().toString(), res.code(), contentType, null, Instant.now());
                return new FetchResult(link, doc);
            }

        } catch (Throwable e) {
            String errorName = e.getClass().getSimpleName();
            if (errorName == null || errorName.isBlank()) {
                errorName = "UnknownError";
            }

            Link link = new Link(url, null, 0, null, errorName, Instant.now());

            return new FetchResult(link, null);
        }
    }

    // =========================================================
    // Utility pengelola URL
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
    // Helpers (stream to controller)
    // =========================================================
    private void sendStatus(CheckingStatus status) {
        if (statusConsumer != null) {
            Platform.runLater(() -> statusConsumer.accept(status));
        }
    }

    private void sendTotalLink(String url) {
        if (totalLinkConsumer != null)
            Platform.runLater(() -> totalLinkConsumer.accept(url));
    }

    private void sendWebpageLink(Link link) {
        if (webpageLinkConsumer != null)
            Platform.runLater(() -> webpageLinkConsumer.accept(link));
    }

    private void sendBrokenLink(Link link) {
        if (brokenLinkConsumer != null)
            Platform.runLater(() -> brokenLinkConsumer.accept(link));
    }
}