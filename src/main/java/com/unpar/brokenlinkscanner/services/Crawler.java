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

public class Crawler {

    private final Queue<Link> frontier = new ConcurrentLinkedQueue<>();

    private final Map<String, Link> repositories = new ConcurrentHashMap<>();

    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    private final LinkReceiver receiver;

    private volatile boolean isStopped;

    private ExecutorService executor;

    private String rootHost;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private static final int MAX_LINKS = 2000;

    public Crawler(LinkReceiver receiver) {
        this.receiver = receiver;
    }

    public void start(String seedUrl) {
        isStopped = false;
        repositories.clear();
        rateLimiters.clear();
        frontier.clear();

        if (executor != null) {
            executor.shutdownNow();
        }
        executor = Executors.newVirtualThreadPerTaskExecutor();

        rootHost = UrlHandler.getHost(seedUrl);

        frontier.offer(new Link(seedUrl));

        while (!isStopped && !frontier.isEmpty() && repositories.size() < MAX_LINKS) {

            Link webpageLink = frontier.poll();

            if (webpageLink == null) {
                return;
            }

            Document html = checkLink(webpageLink, true);

            if (!webpageLink.isWebpage() || html == null) {
                continue;
            }

            Map<Link, String> linksOnWebpage = extractLink(html);

            List<Callable<Void>> tasks = new ArrayList<>();

            for (var entry : linksOnWebpage.entrySet()) {
                if (isStopped) {
                    return;
                }

                Link link = entry.getKey();
                String anchorText = entry.getValue();

                Link existingLink = repositories.get(link.getUrl());
                if (existingLink != null) {
                    existingLink.addWebpageSource(webpageLink, anchorText);
                    continue;
                } else {
                    link.addWebpageSource(webpageLink, anchorText);
                }

                if (UrlHandler.getHost(link.getUrl()).equalsIgnoreCase(rootHost)) {
                    frontier.offer(link);
                } else {
                    tasks.add(() -> {
                        if (isStopped) return null;
                        checkLink(link, false);
                        return null;
                    });

                }
            }

            if (!tasks.isEmpty()) {
                try {
                    executor.invokeAll(tasks);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void stop() {
        isStopped = true;
        frontier.clear();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private Document checkLink(Link link, boolean isParseDoc) {
        if (repositories.get(link.getUrl()) != null || repositories.size() > MAX_LINKS) {
            return null;
        }

        try {
            HttpResponse<?> res = fetch(link.getUrl(), isParseDoc);

            link.setFinalUrl(res.uri().toString());
            link.setContentType(res.headers().firstValue("Content-Type").orElse("").toLowerCase());
            link.setStatusCode(res.statusCode());

            Document html = null;
            boolean isOk = link.getStatusCode() == 200 && res.body() != null;
            boolean isSameHost = UrlHandler.getHost(link.getFinalUrl()).equals(rootHost);

            if (isParseDoc && isOk && isSameHost) {
                try {
                    String body = (String) res.body();

                    html = Jsoup.parse(body, link.getFinalUrl());

                    link.setIsWebpage(true);
                } catch (Exception ignore) {
                    html = null;
                }
            }

            return html;
        } catch (Throwable e) {
            // link.setError(e.getClass().getSimpleName());
            link.setError(ErrorHandler.getExceptionError(e));
            return null;
        } finally {
            Link existing = repositories.putIfAbsent(link.getUrl(), link);
            if (existing == null) {
                receiver.receive(link);
            }
        }
    }

    private Map<Link, String> extractLink(Document HTML) {

        Map<Link, String> result = new HashMap<>();

        for (Element a : HTML.select("a[href]")) {

            String absoluteUrl = a.absUrl("href");

            if (absoluteUrl.isEmpty()) {
                continue;
            }

            String normalizedUrl = UrlHandler.normalizeUrl(absoluteUrl, false);

            if (normalizedUrl == null) {
                continue;
            }

            result.putIfAbsent(new Link(normalizedUrl), a.text().trim());
        }

        return result;
    }

    private HttpResponse<?> fetch(String url, boolean isNeedBody) throws Exception {
        RateLimiter limiter = rateLimiters.computeIfAbsent(UrlHandler.getHost(url), h -> new RateLimiter());
        limiter.delay();

        HttpRequest request = HttpRequest.newBuilder()
                // URL target
                .uri(URI.create(url))
                // Metode HTTP
                .GET()
                // Header
                .header("User-Agent", "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2)")
                // Request Timeout
                .timeout(Duration.ofSeconds(20)).build();

        HttpResponse<?> response;

        if (isNeedBody) {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } else {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        }

        return response;
    }

    public boolean isStoppedByUser() {
        return isStopped;
    }

}
