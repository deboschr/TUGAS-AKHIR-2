package com.unpar.brokenlinkchecker.utils;

import com.unpar.brokenlinkchecker.cores.Crawler;
import com.unpar.brokenlinkchecker.models.Link;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpHandler {
    private static final String USER_AGENT = "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)";
    private static final OkHttpClient OK_HTTP = new OkHttpClient.Builder().followRedirects(true) // Ikuti redirect dari server
            .connectTimeout(10, TimeUnit.SECONDS) // Batas waktu untuk membangun koneksi ke server
            .readTimeout(20, TimeUnit.SECONDS) // Batas waktu untuk membaca respons dari server
            .build();

    private static final Map<Integer, String> STATUS_MAP = Map.ofEntries(
            // 4xx - Client Errors
            Map.entry(400, "400 Bad Request"), Map.entry(401, "401 Unauthorized"), Map.entry(402, "402 Payment Required"), Map.entry(403, "403 Forbidden"), Map.entry(404, "404 Not Found"), Map.entry(405, "405 Method Not Allowed"), Map.entry(406, "406 Not Acceptable"), Map.entry(407, "407 Proxy Authentication Required"), Map.entry(408, "408 Request Timeout"), Map.entry(409, "409 Conflict"), Map.entry(410, "410 Gone"), Map.entry(411, "411 Length Required"), Map.entry(412, "412 Precondition Failed"), Map.entry(413, "413 Content Too Large"), Map.entry(414, "414 URI Too Long"), Map.entry(415, "415 Unsupported Media Type"), Map.entry(416, "416 Range Not Satisfiable"), Map.entry(417, "417 Expectation Failed"), Map.entry(418, "418 I'm a teapot"), Map.entry(421, "421 Misdirected Request"), Map.entry(422, "422 Unprocessable Content"), Map.entry(423, "423 Locked"), Map.entry(424, "424 Failed Dependency"), Map.entry(425, "425 Too Early"), Map.entry(426, "426 Upgrade Required"), Map.entry(428, "428 Precondition Required"), Map.entry(429, "429 Too Many Requests"), Map.entry(431, "431 Request Header Fields Too Large"), Map.entry(451, "451 Unavailable For Legal Reasons"),
            // 5xx - Server Errors
            Map.entry(500, "500 Internal Server Error"), Map.entry(501, "501 Not Implemented"), Map.entry(502, "502 Bad Gateway"), Map.entry(503, "503 Service Unavailable"), Map.entry(504, "504 Gateway Timeout"), Map.entry(505, "505 HTTP Version Not Supported"), Map.entry(506, "506 Variant Also Negotiates"), Map.entry(507, "507 Insufficient Storage"), Map.entry(508, "508 Loop Detected"), Map.entry(510, "510 Not Extended"), Map.entry(511, "511 Network Authentication Required"));


    public static Document fetch(Link link, Boolean isParseDoc) {
        try {
            Request request = new Request.Builder().url(link.getUrl()).header("User-Agent", USER_AGENT).get().build();

            try (Response res = OK_HTTP.newCall(request).execute()) {

                Document doc = null;
                int statusCode = res.code();
                String contentType = res.header("Content-Type", "");
                String finalUrl = res.request().url().toString();

                assert contentType != null;
                boolean isHtml = contentType.toLowerCase().contains("text/html");
                if (isParseDoc && statusCode == 200 && isHtml) {
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
            if (errorName.isBlank()) {
                errorName = "UnknownError";
            }

            link.setError(errorName);

            return null;
        }
    }

    public static String getErrorStatus(int statusCode) {
        // kalau 1xx, 2xx & 3xx maka artinya bukan error, jadi return null
        if (statusCode >= 100 && statusCode < 400) {
            return null;
        }

        return STATUS_MAP.getOrDefault(statusCode, String.valueOf(statusCode));
    }
}
