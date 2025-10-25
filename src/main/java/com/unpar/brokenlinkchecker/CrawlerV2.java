package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.*;
import javafx.application.Platform;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CrawlerV2 {

   // ===================== Konstanta =====================
   private static final String USER_AGENT = "BrokenLinkChecker/1.0 (+https://github.com/deboschr/TUGAS-AKHIR-2)";
   private static final int TIMEOUT = 10000;

   private static final OkHttpClient OK_HTTP = new OkHttpClient.Builder()
         .followRedirects(true)
         .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
         .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
         .build();

   // ===================== Callback =====================
   private final Consumer<CheckingStatus> checkingStatusConsumer;
   private final Consumer<Link> linkConsumer;

   // ===================== State Internal =====================
   private final Queue<Link> frontier = new ArrayDeque<>();
   private final Map<String, Link> repositories = new HashMap<>();

   private String rootHost;
   private volatile boolean isRunning = false;

   public CrawlerV2(Consumer<CheckingStatus> checkingStatusConsumer, Consumer<Link> linkConsumer) {
      this.checkingStatusConsumer = checkingStatusConsumer;
      this.linkConsumer = linkConsumer;
   }

   // ============================================================
   // Entry point
   // ============================================================
   public void start(String seedUrl) {
      isRunning = true;
      send(checkingStatusConsumer, CheckingStatus.CHECKING);

      repositories.clear();
      frontier.clear();

      rootHost = getHost(seedUrl);
      if (rootHost == null) {
         send(checkingStatusConsumer, CheckingStatus.STOPPED);
         return;
      }

      Link seed = getOrCreateLink(seedUrl);
      frontier.offer(seed);

      while (isRunning && !frontier.isEmpty()) {
         Link current = frontier.poll();
         if (current == null)
            continue;

         // skip jika sudah pernah dikunjungi
         if (current.getStatusCode() != 0)
            continue;

         FetchResult result = fetchUrl(current, true);
         Document doc = result.document();

         // kirim link apapun hasilnya ke UI
         send(linkConsumer, current);

         // kalau gagal fetch / bukan HTML / beda host → stop
         if (current.getStatusCode() >= 400 || doc == null)
            continue;
         String host = getHost(current.getFinalUrl());
         if (host == null || !host.equals(rootHost))
            continue;

         // ekstrak semua link dari halaman
         Map<String, String> outlinks = extractUrl(doc);
         current.clearConnection();

         for (Map.Entry<String, String> entry : outlinks.entrySet()) {
            String url = entry.getKey();
            String anchor = entry.getValue();
            Link target = getOrCreateLink(url);

            // relasi dua arah
            target.setConnection(current, anchor);

            String targetHost = getHost(url);
            if (targetHost != null && targetHost.equals(rootHost)) {
               frontier.offer(target);
            } else if (target.getStatusCode() == 0) {
               // langsung check eksternal / non-html link
               fetchUrl(target, false);
               send(linkConsumer, target);
            }
         }
      }

      if (isRunning)
         send(checkingStatusConsumer, CheckingStatus.COMPLETED);
   }

   public void stop() {
      isRunning = false;
      send(checkingStatusConsumer, CheckingStatus.STOPPED);
   }

   // ============================================================
   // Core fetching
   // ============================================================
   private FetchResult fetchUrl(Link link, boolean parseHtml) {
      try {
         Request req = new Request.Builder()
               .url(link.getUrl())
               .header("User-Agent", USER_AGENT)
               .get()
               .build();

         try (Response res = OK_HTTP.newCall(req).execute()) {
            String contentType = res.header("Content-Type", "");
            boolean isHtml = contentType != null && contentType.toLowerCase().contains("text/html");

            Document doc = null;
            if (parseHtml && res.code() == 200 && isHtml && res.body() != null) {
               String html = res.body().string();
               doc = Jsoup.parse(html, res.request().url().toString());
            }

            link.setFinalUrl(res.request().url().toString());
            link.setStatusCode(res.code());
            link.setContentType(contentType);

            return new FetchResult(link, doc);
         }

      } catch (Throwable e) {
         link.setError(e.getClass().getSimpleName());
         return new FetchResult(link, null);
      }
   }

   // ============================================================
   // Utility
   // ============================================================
   private Map<String, String> extractUrl(Document doc) {
      Map<String, String> map = new HashMap<>();
      for (Element a : doc.select("a[href]")) {
         String abs = a.attr("abs:href");
         String normalized = normalizeUrl(abs);
         if (normalized != null) {
            map.putIfAbsent(normalized, a.text().trim());
         }
      }
      return map;
   }

   private String normalizeUrl(String raw) {
      if (raw == null || raw.isBlank())
         return null;
      try {
         URI uri = URI.create(raw.trim());
         String scheme = uri.getScheme();
         if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")))
            return null;
         String host = uri.getHost();
         if (host == null)
            return null;
         int port = uri.getPort();
         if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443))
            port = -1;
         URI cleaned = new URI(scheme.toLowerCase(), null, host.toLowerCase(), port, uri.getPath(), uri.getQuery(),
               null);
         return cleaned.toASCIIString();
      } catch (Exception e) {
         return null;
      }
   }

   private String getHost(String url) {
      try {
         URI uri = URI.create(url.trim());
         String host = uri.getHost();
         if (host == null)
            return null;
         return IDN.toASCII(host.toLowerCase());
      } catch (Exception e) {
         return null;
      }
   }

   // ============================================================
   // Helpers
   // ============================================================
   private Link getOrCreateLink(String url) {
      return repositories.computeIfAbsent(url, Link::new);
   }

   private <T> void send(Consumer<T> consumer, T data) {
      if (consumer == null)
         return;
      Platform.runLater(() -> consumer.accept(data));
   }
}
