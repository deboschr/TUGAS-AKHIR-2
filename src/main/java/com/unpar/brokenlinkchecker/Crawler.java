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

public class Crawler {
   // ===================== Konstanta =====================
   private static final String USER_AGENT = "BrokenLinkChecker/1.0 (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)";
   private static final int TIMEOUT = 10000;
   private static final OkHttpClient OK_HTTP = new OkHttpClient.Builder()
         .followRedirects(true)
         .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
         .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
         .build();

   // ==================== Internal state ====================
   // Untuk mengidentifikasi webpage
   private String rootHost;

   // Untuk menyimoan antrian url webpage yang akan di crawling (FIFO/BFS)
   private final Queue<String> frontier = new ArrayDeque<>();

   // Untuk menyimpan daftar unik setiap URL yang ditemukan
   private final Map<String, Link> repositories = new HashMap<>();

   // Untuk menandai status proses
   private volatile boolean isRunning = false;

   // ===================== Callback =====================
   // Untuk mengirim status pengecekan
   private final Consumer<CheckingStatus> checkingStatusConsumer;

   // Untuk mengirim link yang ditemukan
   private final Consumer<Link> linkConsumer;

   public Crawler(Consumer<CheckingStatus> checkingStatusConsumer, Consumer<Link> linkConsumer) {
      this.checkingStatusConsumer = checkingStatusConsumer;
      this.linkConsumer = linkConsumer;
   }

   // =========================================================
   // Core
   // =========================================================

   public void start(String seedUrl) {

   }

   public void stop() {

   }

   private Document fetchUrl(Link link, Boolean isParseDoc) {

      return null;
   }

   // =========================================================
   // Utility
   // =========================================================

   private Map<String, String> extractUrl(Document doc) {

      Map<String, String> urlMap = new HashMap<>();

      for (Element a : doc.select("a[href]")) {

         String absoluteUrl = a.attr("abs:href");

         String normalizedUrl = normalizeUrl(absoluteUrl);

         if (normalizedUrl != null) {
            urlMap.putIfAbsent(normalizedUrl, a.text().trim());
         }
      }

      return urlMap;
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
}
