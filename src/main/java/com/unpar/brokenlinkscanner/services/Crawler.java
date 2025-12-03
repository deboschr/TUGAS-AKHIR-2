package com.unpar.brokenlinkscanner.services;

import com.unpar.brokenlinkscanner.models.Link;
import com.unpar.brokenlinkscanner.utils.HttpHandler;
import com.unpar.brokenlinkscanner.utils.LinkReceiver;
import com.unpar.brokenlinkscanner.utils.RateLimiter;
import com.unpar.brokenlinkscanner.utils.UrlHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

public class Crawler {

    private final Queue<Link> frontier = new ConcurrentLinkedQueue<>();

    private final Map<String, Link> repositories = new ConcurrentHashMap<>();

    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    private final LinkReceiver receiver;

    private volatile boolean isStopped = false;

    private String rootHost;

    private static final int MAX_LINKS = 1000;

    public Crawler(LinkReceiver receiver) {
        this.receiver = receiver;
    }

    public void start(String seedUrl) {
        isStopped = false;
        repositories.clear();
        rateLimiters.clear();
        frontier.clear();

        rootHost = UrlHandler.getHost(seedUrl);

        frontier.offer(new Link(seedUrl));

        while (!isStopped && !frontier.isEmpty() && repositories.size() < MAX_LINKS) {

            Link currLink = frontier.poll();

            if (currLink == null) {
                continue;
            }

            Document HTML = checkLink(currLink, true);

            if (!currLink.isWebpage() || HTML == null) {
                continue;
            }

            Map<Link, String> linksOnWebpage = extractLink(HTML);

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
                        existingLink.addRelation(currLink, anchorText);
                        continue;
                    }

                    link.addRelation(currLink, anchorText);

                    if (UrlHandler.getHost(link.getUrl()).equalsIgnoreCase(rootHost)) {
                        frontier.offer(link);
                    } else {
                        executor.submit(() -> {
                            checkLink(link, false);
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

    public void stop() {
        isStopped = true;
        frontier.clear();
    }

    private Document checkLink(Link link, boolean isParseDoc) {
        try {
            if (repositories.get(link.getUrl()) != null || repositories.size() > MAX_LINKS) {
                return null;
            }

            RateLimiter limiter = rateLimiters.computeIfAbsent(UrlHandler.getHost(link.getUrl()), h -> new RateLimiter());
            limiter.delay();

            HttpResponse<?> res = HttpHandler.fetch(link.getUrl(), isParseDoc);
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

            link.setError(e.getClass().getSimpleName());

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

    public boolean isStoppedByUser() {
        return isStopped;
    }

}
