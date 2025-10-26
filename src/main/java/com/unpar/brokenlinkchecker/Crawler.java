package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.Link;
import com.unpar.brokenlinkchecker.model.CheckingStatus;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
   private static final String USER_AGENT = "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)";
   private static final OkHttpClient OK_HTTP = new OkHttpClient.Builder()
         .followRedirects(true)
         .connectTimeout(10, TimeUnit.SECONDS) // Batas waktu untuk membangun koneksi ke server
         .readTimeout(20, TimeUnit.SECONDS) // Batas waktu untuk membaca respons dari server
         .build();

   // ==================== Internal state ====================
   // Untuk mengidentifikasi webpage
   private String rootHost;

   // Untuk menyimoan antrian URL webpage yang akan di crawling (FIFO/BFS)
   private final Queue<Link> frontier = new ArrayDeque<>();

   // Untuk menyimpan daftar unik setiap URL yang ditemukan
   private final Map<String, Link> repositories = new ConcurrentHashMap<>();

   // Untuk menyimpan rate limiter per host biar tiap host punya batas request-nya
   private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

   // Untuk menandai status proses
   private volatile boolean isRunning = false;

   // ===================== Callback =====================

   // Untuk mengirim link yang ditemukan
   private final Consumer<Link> linkConsumer;

   public Crawler(Consumer<Link> linkConsumer) {
      this.linkConsumer = linkConsumer;
   }

   // =========================================================
   // Core
   // =========================================================

   public void start(String seedUrl) {
      // set status
      isRunning = true;
      send(checkingStatusConsumer, CheckingStatus.CHECKING);

      // reset penyimpanan
      repositories.clear();
      frontier.clear();
      rateLimiters.clear();

      // set init value
      rootHost = getHostUrl(seedUrl);
      frontier.offer(new Link(seedUrl));

      while (isRunning && !frontier.isEmpty()) {
         // Ambil link paling depan (FIFO)
         Link webpageLink = frontier.poll();

         if (repositories.putIfAbsent(seedUrl, webpageLink) != null) {
            continue;
         }

         // Fetch dan parse body dari webpage link
         Document doc = fetchUrl(webpageLink, true);

         // Kirim link ke controller (all link)
         send(linkConsumer, webpageLink);

         if (webpageLink.getStatusCode() >= 400 || webpageLink.getError() != "") {
            continue;
         }

         // Skip kalau dokumen kosong (bukan HTML) atau kalau beda host
         String finalUrlHost = getHostUrl(webpageLink.getFinalUrl());
         if (doc == null || !finalUrlHost.equalsIgnoreCase(rootHost)) {
            continue;
         }

         // Ekstrak seluruh url yang ada di webpage
         Map<String, String> linksOnWebpage = extractUrl(doc);

         // Jalankan pemrosesan tiap link di virtual thread terpisah karena nanti
         // fetching url itu I/O bound
         try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            linksOnWebpage.forEach((url, anchorText) -> executor.submit(() -> {

               // Cek dulu apakah URL ini sudah ada di repositories
               Link existingLink = repositories.get(url);
               if (existingLink != null) {
                  // Pake synchronized untuk mencegah race condition pada objek existingLink
                  synchronized (existingLink) {
                     existingLink.setConnection(webpageLink, anchorText);
                  }
                  // Skip ke iterasi berikutnya
                  return;
               }

               // Ambil hostnya buat dibandingan sama host seed url
               String host = getHostUrl(url);

               // Buat objek link baru dan bikin koneksi dengan webpage
               Link link = new Link(url);
               link.setConnection(webpageLink, anchorText);

               // Kalau hostnya sama dengan seed url, maka masukan ke daftar yang akan di parse
               if (host.equalsIgnoreCase(rootHost)) {
                  // Masukan ke antrian paling belakang
                  frontier.offer(link);
               }
               // Kalau tidak maka lansung kunjungi/cek
               else {
                  // Fetch URL tanpa parse, karena kita ga butuh doc
                  fetchUrl(link, false);

                  // Simpan ke repositories kalau belum ada
                  repositories.putIfAbsent(url, link);

                  // Kirim link ke controller (all link)
                  send(linkConsumer, link);
               }

            }));

            // Tunggu semua task di halaman ini selesai sebelum lanjut ke halaman BFS
            // berikutnya di frontier
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);

         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

   private Document fetchUrl(Link link, Boolean isParseDoc) {
      try {
         String host = getHostUrl(link.getUrl());
         RateLimiter limiter = rateLimiters.computeIfAbsent(host, h -> new RateLimiter());
         limiter.delay();

         Request request = new Request.Builder()
               .url(link.getUrl())
               .header("User-Agent", USER_AGENT)
               .get()
               .build();

         try (Response res = OK_HTTP.newCall(request).execute()) {

            Document doc = null;
            int statusCode = res.code();
            String contentType = res.header("Content-Type", "");
            String finalUrl = res.request().url().toString();

            boolean isHtml = contentType.toLowerCase().contains("text/html");
            if (isParseDoc && statusCode == 200 && isHtml && res.body() != null) {
               try {
                  String html = res.body().string();

                  doc = Jsoup.parse(html, finalUrl);
               } catch (Exception parseErr) {
                  doc = null;
               }
            }

            link.setFinalUrl(finalUrl);
            link.setContentType(contentType);
            link.setStatusCode(statusCode);

            return doc;
         }

      } catch (Throwable e) {
         String errorName = e.getClass().getSimpleName();
         if (errorName == null || errorName.isBlank()) {
            errorName = "UnknownError";
         }

         link.setError(errorName);

         return null;
      }
   }

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

   // =========================================================
   // Utility
   // =========================================================

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
            return "";
         }

         // konversi ke format ASCII untuk domain internasional
         return IDN.toASCII(host.toLowerCase());
      } catch (IllegalArgumentException e) {
         // URL tidak valid secara sintaks
         return "";
      }
   }

   /**
    * Method ini dipakai buat ngirim data hasil proses balik ke Controller, tapi
    * dengan cara yang aman dari thread lain.
    * 
    * - Kalau aplikasi lagi jalanin proses di background thread (misalnya
    * crawling), kita gak bisa langsung ubah komponen di UI, karena JavaFX cuma
    * boleh ubah UI dari thread khusus yang namanya "JavaFX Application Thread".
    * - Nah, biar aman dari thread conflict, kita panggil `Platform.runLater()`,
    * supaya kode di dalamnya dijalankan nanti di thread UI itu.
    * 
    * 
    * @param <T>      tipe data yang akan dikirim
    * @param consumer objek Consumer yang menerima data dari Crawler untuk diproses
    *                 di Controller
    * @param data     data yang akan dikirim ke consumer
    */
   private <T> void send(Consumer<T> consumer, T data) {
      if (consumer != null) {
         Platform.runLater(() -> consumer.accept(data));
      }
   }

   // =========================================================
   // Rate Limiter
   // =========================================================

   private static class RateLimiter {
      // Waktu jarak antar request, karena 500ms maka hanya 2 req per detik
      private static final long INTERVAL = 500L;

      // Waktu dari request terakhir
      private volatile long lastRequestTime = 0L;

      /**
       * Method utama untuk mengatur delay atau jarak waktu antar satu request ke
       * request yang lain.
       *
       * synchronized artinya cuma satu thread dalam satu waktu yang bisa menjalankan
       * method ini untuk 1 host yang sama. Jadi kalau ada 3 thread yang akses host
       * yang sama barengan, mereka akan ngantri.
       */
      public synchronized void delay() {
         // Waktu saat ini dalam epoch
         long now = System.currentTimeMillis();

         // Waktu untuk menggu
         long waitTime = lastRequestTime + INTERVAL - now;

         // Kalau masih belum lewat 500 ms dari request terakhir, tunggu dulu
         if (waitTime > 0) {
            try {
               Thread.sleep(waitTime);
            } catch (InterruptedException e) {
               // Kalau thread dibatalkan, set flag interrupted biar caller tahu
               Thread.currentThread().interrupt();
            }
         }

         // update waktu terakhir dengan waktu sekarang
         lastRequestTime = System.currentTimeMillis();
      }
   }
}
