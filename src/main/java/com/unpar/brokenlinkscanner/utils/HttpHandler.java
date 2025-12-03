package com.unpar.brokenlinkscanner.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HttpHandler {

    private static final long CONNECTION_TIMEOUT = 12L;
    private static final long REQUEST_TIMEOUT = 10L;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT)).build();

    private static final String USER_AGENT = "BrokenLinkChecker (+https://github.com/deboschr/TUGAS-AKHIR-2; contact: 6182001060@student.unpar.ac.id)";

    private static final Map<Integer, String> STATUS_MAP = Map.ofEntries(
            // 4xx - Client Errors
            Map.entry(400, "400 Bad Request"), Map.entry(401, "401 Unauthorized"), Map.entry(402, "402 Payment Required"), Map.entry(403, "403 Forbidden"), Map.entry(404, "404 Not Found"), Map.entry(405, "405 Method Not Allowed"), Map.entry(406, "406 Not Acceptable"), Map.entry(407, "407 Proxy Authentication Required"), Map.entry(408, "408 Request Timeout"), Map.entry(409, "409 Conflict"), Map.entry(410, "410 Gone"), Map.entry(411, "411 Length Required"), Map.entry(412, "412 Precondition Failed"), Map.entry(413, "413 Content Too Large"), Map.entry(414, "414 URI Too Long"), Map.entry(415, "415 Unsupported Media Type"), Map.entry(416, "416 Range Not Satisfiable"), Map.entry(417, "417 Expectation Failed"), Map.entry(418, "418 I'm a teapot"), Map.entry(421, "421 Misdirected Request"), Map.entry(422, "422 Unprocessable Content"), Map.entry(423, "423 Locked"), Map.entry(424, "424 Failed Dependency"), Map.entry(425, "425 Too Early"), Map.entry(426, "426 Upgrade Required"), Map.entry(428, "428 Precondition Required"), Map.entry(429, "429 Too Many Requests"), Map.entry(431, "431 Request Header Fields Too Large"), Map.entry(451, "451 Unavailable For Legal Reasons"),
            // 5xx - Server Errors
            Map.entry(500, "500 Internal Server Error"), Map.entry(501, "501 Not Implemented"), Map.entry(502, "502 Bad Gateway"), Map.entry(503, "503 Service Unavailable"), Map.entry(504, "504 Gateway Timeout"), Map.entry(505, "505 HTTP Version Not Supported"), Map.entry(506, "506 Variant Also Negotiates"), Map.entry(507, "507 Insufficient Storage"), Map.entry(508, "508 Loop Detected"), Map.entry(510, "510 Not Extended"), Map.entry(511, "511 Network Authentication Required"));

    public static HttpResponse<?> fetch(String url, boolean isNeedBody) throws Exception {

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().header("User-Agent", USER_AGENT).timeout(Duration.ofSeconds(REQUEST_TIMEOUT)).build();

        HttpResponse<?> response;

        if (isNeedBody) {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } else {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        }

        return response;
    }

    public static String getStatusError(int statusCode) {

        if (statusCode >= 100 && statusCode < 400) {
            return null;
        }

        return STATUS_MAP.getOrDefault(statusCode, String.valueOf(statusCode));
    }

    public static boolean isStandardError(int statusCode) {
        return STATUS_MAP.containsKey(statusCode);
    }
}
