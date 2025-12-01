package com.unpar.brokenlinkscanner.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HttpHandler {
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
            .connectTimeout(Duration.ofSeconds(10)).build();

    /**
     * Untuk menyimpan request header user-agent, dipake biar server website tujuan
     * tahu siapa yang melakukan request, ini salah satu implementasi etika crawling
     */
    private static final String USER_AGENT = "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)";

    /**
     * Untuk menyimpan HTTP status yaitu kode status + reason phrase
     *
     * Struktur data Map dipakai untuk menyimpan pasangan key dan value:
     * - Key : kode status HTTP
     * - Value : pesan errornya (kode status + reason phrase)
     */
    private static final Map<Integer, String> STATUS_MAP = Map.ofEntries(
            // 4xx - Client Errors
            Map.entry(400, "400 Bad Request"), Map.entry(401, "401 Unauthorized"), Map.entry(402, "402 Payment Required"), Map.entry(403, "403 Forbidden"), Map.entry(404, "404 Not Found"), Map.entry(405, "405 Method Not Allowed"), Map.entry(406, "406 Not Acceptable"), Map.entry(407, "407 Proxy Authentication Required"), Map.entry(408, "408 Request Timeout"), Map.entry(409, "409 Conflict"), Map.entry(410, "410 Gone"), Map.entry(411, "411 Length Required"), Map.entry(412, "412 Precondition Failed"), Map.entry(413, "413 Content Too Large"), Map.entry(414, "414 URI Too Long"), Map.entry(415, "415 Unsupported Media Type"), Map.entry(416, "416 Range Not Satisfiable"), Map.entry(417, "417 Expectation Failed"), Map.entry(418, "418 I'm a teapot"), Map.entry(421, "421 Misdirected Request"), Map.entry(422, "422 Unprocessable Content"), Map.entry(423, "423 Locked"), Map.entry(424, "424 Failed Dependency"), Map.entry(425, "425 Too Early"), Map.entry(426, "426 Upgrade Required"), Map.entry(428, "428 Precondition Required"), Map.entry(429, "429 Too Many Requests"), Map.entry(431, "431 Request Header Fields Too Large"), Map.entry(451, "451 Unavailable For Legal Reasons"),
            // 5xx - Server Errors
            Map.entry(500, "500 Internal Server Error"), Map.entry(501, "501 Not Implemented"), Map.entry(502, "502 Bad Gateway"), Map.entry(503, "503 Service Unavailable"), Map.entry(504, "504 Gateway Timeout"), Map.entry(505, "505 HTTP Version Not Supported"), Map.entry(506, "506 Variant Also Negotiates"), Map.entry(507, "507 Insufficient Storage"), Map.entry(508, "508 Loop Detected"), Map.entry(510, "510 Not Extended"), Map.entry(511, "511 Network Authentication Required"));

    public static HttpResponse<?> fetch(String url, boolean needResponseBody) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                // URL tujuan
                .uri(URI.create(url))
                // Menggunakan metode GET untuk fetching
                .GET()
                // Header biar server tahu yg minta request adalah aplikasi kita
                .header("User-Agent", USER_AGENT)
                // Timeout total request (connect + read)
                .timeout(Duration.ofSeconds(10))
                // Bikin objek request
                .build();

        HttpResponse<?> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());

        int statusCode = response.statusCode();
        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("")
                .toLowerCase();

        if (needResponseBody && statusCode == 200 && contentType.contains("text/html")) {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        }

        return response;
    }

    public static String getStatusError(int statusCode) {
        // Kalau 1xx, 2xx & 3xx maka artinya bukan error
        if (statusCode >= 100 && statusCode < 400) {
            return null;
        }

        // Kalau statusCode tidak ada di STATUS_MAP, maka yang dikembalikan adalah statusCode dalam bentuk string (default).
        return STATUS_MAP.getOrDefault(statusCode, String.valueOf(statusCode));
    }

    public static boolean isStandardError(int statusCode) {
        return STATUS_MAP.containsKey(statusCode);
    }

}
