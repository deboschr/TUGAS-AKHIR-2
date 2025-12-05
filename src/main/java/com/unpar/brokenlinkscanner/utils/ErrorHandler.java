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

        // ========== 0. Jika thread dihentikan ==========
        if (e instanceof InterruptedException ||
                e instanceof CancellationException) {
            return "";
        }

        // Ambil exception utama + cause hingga paling dalam
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String name = root.getClass().getName();
        String simple = root.getClass().getSimpleName();
        String msg = root.getMessage() != null ? root.getMessage().toLowerCase() : "";

        // ========== 1. Timeout ==========
        if (simple.contains("HttpTimeoutException")) return "Request Timeout";
        if (simple.contains("HttpConnectTimeoutException")) return "Connection Timeout";

        // ========== 2. Host errors ==========
        if (root instanceof UnknownHostException || msg.contains("unknown host")) return "Host Not Found";
        if (msg.contains("no such host")) return "Host Not Found";
        if (simple.equals("UnresolvedAddressException")) return "Host Not Found";

        // ========== 3. Routing / Network ==========
        if (msg.contains("no route to host")) return "No Route To Host";
        if (msg.contains("network is unreachable")) return "Network Unreachable";

        // ========== 4. CONNECTION ERROR ==========
        if (msg.contains("refused")) return "Connection Refused";
        if (msg.contains("connection reset")) return "Connection Reset";
        if (msg.contains("broken pipe")) return "Connection Closed";
        if (root instanceof ConnectException) return "Connection Error";

        // ========== 6. SSL / Certificate ==========
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

        // ========== 7. Invalid URL ==========
        if (root instanceof MalformedURLException) return "Invalid URL";
        if (root instanceof MalformedInputException) return "Invalid URL";
        if (root instanceof IllegalArgumentException) return "Invalid URL";

        // ========== 9. IOException generik ==========
        if (root instanceof IOException) return "I/O Error";

        // ========== 10. Fallback: format nama ==========
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
