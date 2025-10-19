package com.unpar.brokenlinkchecker.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitas untuk memetakan kode status HTTP ke kode status + reason phrase.
 */
public class HttpStatus {

    private static final Map<Integer, String> STATUS_MAP = new HashMap<>();

    static {
        // 4xx - Client Errors
        STATUS_MAP.put(400, "400 Bad Request");
        STATUS_MAP.put(401, "401 Unauthorized");
        STATUS_MAP.put(402, "402 Payment Required");
        STATUS_MAP.put(403, "403 Forbidden");
        STATUS_MAP.put(404, "404 Not Found");
        STATUS_MAP.put(405, "405 Method Not Allowed");
        STATUS_MAP.put(406, "406 Not Acceptable");
        STATUS_MAP.put(407, "407 Proxy Authentication Required");
        STATUS_MAP.put(408, "408 Request Timeout");
        STATUS_MAP.put(409, "409 Conflict");
        STATUS_MAP.put(410, "410 Gone");
        STATUS_MAP.put(411, "411 Length Required");
        STATUS_MAP.put(412, "412 Precondition Failed");
        STATUS_MAP.put(413, "413 Content Too Large");
        STATUS_MAP.put(414, "414 URI Too Long");
        STATUS_MAP.put(415, "415 Unsupported Media Type");
        STATUS_MAP.put(416, "416 Range Not Satisfiable");
        STATUS_MAP.put(417, "417 Expectation Failed");
        STATUS_MAP.put(418, "418 I'm a teapot");
        STATUS_MAP.put(421, "421 Misdirected Request");
        STATUS_MAP.put(422, "422 Unprocessable Content");
        STATUS_MAP.put(423, "423 Locked");
        STATUS_MAP.put(424, "424 Failed Dependency");
        STATUS_MAP.put(425, "425 Too Early");
        STATUS_MAP.put(426, "426 Upgrade Required");
        STATUS_MAP.put(428, "428 Precondition Required");
        STATUS_MAP.put(429, "429 Too Many Requests");
        STATUS_MAP.put(431, "431 Request Header Fields Too Large");
        STATUS_MAP.put(451, "451 Unavailable For Legal Reasons");

        // 5xx - Server Errors
        STATUS_MAP.put(500, "500 Internal Server Error");
        STATUS_MAP.put(501, "501 Not Implemented");
        STATUS_MAP.put(502, "502 Bad Gateway");
        STATUS_MAP.put(503, "503 Service Unavailable");
        STATUS_MAP.put(504, "504 Gateway Timeout");
        STATUS_MAP.put(505, "505 HTTP Version Not Supported");
        STATUS_MAP.put(506, "506 Variant Also Negotiates");
        STATUS_MAP.put(507, "507 Insufficient Storage");
        STATUS_MAP.put(508, "508 Loop Detected");
        STATUS_MAP.put(510, "510 Not Extended");
        STATUS_MAP.put(511, "511 Network Authentication Required");
    }

    public static String getStatus(int statusCode) {
        // kalau 1xx, 2xx & 3xx maka artinya bukan error, jadi return null
        if (statusCode >= 100 && statusCode < 400) {
            return null;
        }

        return STATUS_MAP.getOrDefault(statusCode, String.valueOf(statusCode));
    }
}
