package com.unpar.brokenlinkscanner.utils;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.charset.MalformedInputException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.CancellationException;

public class ErrorHandler {
    private static final Map<Integer, String> HTTP_STATUS_MAP = Map.ofEntries(
            // 4xx - Client Errors
            Map.entry(400, "400 Bad Request"), Map.entry(401, "401 Unauthorized"), Map.entry(402, "402 Payment Required"), Map.entry(403, "403 Forbidden"), Map.entry(404, "404 Not Found"), Map.entry(405, "405 Method Not Allowed"), Map.entry(406, "406 Not Acceptable"), Map.entry(407, "407 Proxy Authentication Required"), Map.entry(408, "408 Request Timeout"), Map.entry(409, "409 Conflict"), Map.entry(410, "410 Gone"), Map.entry(411, "411 Length Required"), Map.entry(412, "412 Precondition Failed"), Map.entry(413, "413 Content Too Large"), Map.entry(414, "414 URI Too Long"), Map.entry(415, "415 Unsupported Media Type"), Map.entry(416, "416 Range Not Satisfiable"), Map.entry(417, "417 Expectation Failed"), Map.entry(418, "418 I'm a teapot"), Map.entry(421, "421 Misdirected Request"), Map.entry(422, "422 Unprocessable Content"), Map.entry(423, "423 Locked"), Map.entry(424, "424 Failed Dependency"), Map.entry(425, "425 Too Early"), Map.entry(426, "426 Upgrade Required"), Map.entry(428, "428 Precondition Required"), Map.entry(429, "429 Too Many Requests"), Map.entry(431, "431 Request Header Fields Too Large"), Map.entry(451, "451 Unavailable For Legal Reasons"),
            // 5xx - Server Errors
            Map.entry(500, "500 Internal Server Error"), Map.entry(501, "501 Not Implemented"), Map.entry(502, "502 Bad Gateway"), Map.entry(503, "503 Service Unavailable"), Map.entry(504, "504 Gateway Timeout"), Map.entry(505, "505 HTTP Version Not Supported"), Map.entry(506, "506 Variant Also Negotiates"), Map.entry(507, "507 Insufficient Storage"), Map.entry(508, "508 Loop Detected"), Map.entry(510, "510 Not Extended"), Map.entry(511, "511 Network Authentication Required"));

    public static String getExceptionError(Throwable e) {

        if (e == null) return "";

        // ========== 0. Thread dihentikan ==========
        if (e instanceof InterruptedException || e instanceof CancellationException) {
            return "";
        }

        // Ambil exception terluar (top) dan root cause
        Throwable top = e;
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String topName = top.getClass().getName();
        String topSimple = top.getClass().getSimpleName();
        String topMsg = top.getMessage() != null ? top.getMessage().toLowerCase() : "";

        String name = root.getClass().getName();
        String simple = root.getClass().getSimpleName();
        String msg = root.getMessage() != null ? root.getMessage().toLowerCase() : "";

        // ========== 1. TIMEOUT (gabung semua) ==========
        if (topSimple.contains("HttpTimeoutException") ||
                topSimple.contains("HttpConnectTimeoutException") ||
                simple.contains("HttpTimeoutException") ||
                simple.contains("HttpConnectTimeoutException") ||
                msg.contains("timed out") ||
                topMsg.contains("timed out")) {

            return "Timeout";
        }

        // ========== 2. HOST NOT FOUND ==========
        if (root instanceof UnknownHostException ||
                msg.contains("unknown host") ||
                msg.contains("no such host") ||
                simple.contains("UnresolvedAddressException")) {

            return "Host Not Found";
        }

        // ========== 4. CONNECTION ERRORS ==========
        if (msg.contains("refused")) return "Connection Refused";
        if (msg.contains("connection reset")) return "Connection Reset";
        if (msg.contains("broken pipe")) return "Connection Closed";

        // ========== 5. SSL ERRORS  ==========
        if (root instanceof SSLHandshakeException ||
                root instanceof CertificateException ||
                name.contains("SunCertPathBuilderException") ||
                msg.contains("certificate") ||
                msg.contains("pkix path") ||
                msg.contains("unable to find valid certification path") ||
                msg.contains("ssl") ||
                msg.contains("tls")) {

            return "SSL Error";
        }

        // ========== 6. INVALID URL ==========
        if (root instanceof MalformedURLException ||
                root instanceof MalformedInputException ||
                root instanceof IllegalArgumentException) {

            return "Invalid URL";
        }
        
        // ========== 8. FALLBACK ==========
        return simple.replaceAll("(.)([A-Z])", "$1 $2");
    }




    public static String getHttpError(int statusCode) {
        // Jika status code bukan termasuk error
        if (statusCode >= 100 && statusCode < 400) {
            return null;
        }

        return HTTP_STATUS_MAP.getOrDefault(statusCode, String.valueOf(statusCode));
    }


    public static boolean isHttpError(int statusCode) {
        return HTTP_STATUS_MAP.containsKey(statusCode);
    }
}
