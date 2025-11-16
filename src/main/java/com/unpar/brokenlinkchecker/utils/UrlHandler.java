package com.unpar.brokenlinkchecker.utils;

import java.net.IDN;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;

public class UrlHandler {

    /**
     * Validasi dan normalisasi URL (misalnya untuk seed URL).
     *
     * Aturan:
     * 1. Wajib punya scheme (http / https)
     * 2. Wajib punya host
     * 3. Hapus port default (80 / 443)
     * 4. Bersihkan path dari dot-segment
     * 5. Hapus fragment (#...)
     *
     * @param rawUrl input mentah
     * @return URL hasil normalisasi atau null jika tidak memenuhi aturan atau URL asli kalau sintaks tidak valid
     */
    public static String normalizeUrl(String rawUrl) {
        // URL tidak boleh null atau string kosong
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return null;
        }

        try {
            // Buat objek URI
            URI uri = new URI(rawUrl.trim());

            // Pisahkan komponen URI
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getRawPath();
            String query = uri.getRawQuery();


            // Scheme wajib ada
            if (scheme == null || scheme.isEmpty()) {
                return null;
            }

            // Scheme wajib wajib HTTP/HTTPS
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                return null;
            }

            // Host wajib ada
            if (host == null || host.isEmpty()) {
                return null;
            }

            // Hapus port default
            if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
                port = -1;
            }

            // Bersihkan path dari dot-segment
            path = normalizePath(path);


            // ===== rakit ulang tanpa fragment =====
            URI cleaned = new URI(scheme.toLowerCase(), null, host.toLowerCase(), port, path, query, null // fragment dihapus
            );

            return cleaned.toASCIIString();

        } catch (Exception e) {
            return rawUrl;
        }
    }

    /**
     * Bersihkan dot-segment (., ..) dari path sesuai RFC 3986 Section 5.2.4.
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        Deque<String> segments = new ArrayDeque<>();

        for (String part : path.split("/")) {
            if (part.equals("") || part.equals(".")) {
                continue;
            } else if (part.equals("..")) {
                if (!segments.isEmpty()) {
                    segments.removeLast();
                }
            } else {
                segments.add(part);
            }
        }

        StringBuilder sb = new StringBuilder();

        for (String seg : segments) {
            sb.append("/").append(seg);
        }

        return sb.isEmpty() ? "/" : sb.toString();
    }

    /**
     * Ambil host dari URL dan ubah ke format ASCII (IDN).
     * Digunakan untuk membandingkan host antar URL.
     */
    public static String getHost(String url) {
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
}
