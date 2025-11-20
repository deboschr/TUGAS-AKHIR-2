package com.unpar.brokenlinkscanner.utils;

import java.net.IDN;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;

public class URLHandler {
    /**
     * Validasi dan normalisasi URL (misalnya untuk seed URL).
     *
     * Aturan:
     * - Wajib punya scheme (http / https)
     * - Wajib punya host
     * - Hapus port default (80 / 443)
     * - Bersihkan path dari dot-segment dan duplikasi garis miring
     * - Hapus fragment (#...)
     * - Hapus user info
     *
     * @param rawUrl input mentah
     * @return URL hasil normalisasi atau null jika tidak memenuhi aturan atau URL
     *         asli kalau sintaks tidak valid
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

            // Scheme wajib HTTP/HTTPS
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

            // Rakit ulang tanpa fragment dan userinfo
            URI cleaned = new URI(
                    scheme.toLowerCase(), // SCHEME
                    null, // USERINFO
                    host.toLowerCase(), // HOST
                    port, // PORT
                    path, // PATH
                    query, // QUERY
                    null // FRAGMENT
            );

            return cleaned.toASCIIString();

        } catch (Exception e) {
            /*
             * Kalau gagal bikin objek URI berarti URL nya invalid sintaks, kita kembaliin
             * URL awal biar nanti error pas pengecekan, karena akan dianggap invalid URL
             */
            return rawUrl;
        }
    }

    /**
     * Mathod buat menormalisasi path
     *
     * Yang di handle di method ini adalah:
     * - "." : artinya di direktory saat ini
     * - ".." : artinya satu level ke direktory atas
     * - duplikasi atau kelebihan garis miring ("/")
     *
     * Contoh hasil:
     * normalizePath("/a/b/../c") ==> "/a/c"
     * normalizePath("/./x//y/") ==> "/x/y"
     * normalizePath("") ==> "/"
     * normalizePath(null) ==> "/"
     *
     * @param path path mentah dari URL
     * @return path yang sudah dinormalisasi
     */
    public static String normalizePath(String path) {

        // Jika path kosong atau null, langsung kembalikan root "/"
        if (path == null || path.isEmpty()) {
            return "/";
        }

        Deque<String> segments = new ArrayDeque<>();

        for (String part : path.split("/")) {

            // Kalau part kosong atau ".", skip karna ga ngaruh ke struktur path
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            // Kalau part "..", hapus satu segmen terakhir (naik satu level)
            else if (part.equals("..")) {
                if (!segments.isEmpty()) {
                    segments.removeLast();
                }
            }
            // Tambahkan part normal ke daftar segmen
            else {
                segments.add(part);
            }
        }

        // Bangun ulang path baru berdasarkan segmen yang udah bersih.
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
