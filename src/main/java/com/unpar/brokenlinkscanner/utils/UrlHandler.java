package com.unpar.brokenlinkscanner.utils;

import java.net.IDN;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Kelas utilitas untuk menangani URL.
 *
 * Kelas ini berisi fungsi-fungsi statis yang digunakan untuk:
 * - mengekstrak host dari URL
 * - melakukan normalisasi URL
 * - melakukan normalisasi path URL
 */
public class UrlHandler {
    /**
     * Method untuk mengambil host dari sebuah URL dalam bentuk ASCII.
     *
     * @param url : URL mentah yang akan diambil host-nya
     * @return host dalam format ASCII (lowercase), atau string kosong jika tidak valid
     */
    public static String getHost(String url) {
        try {
            // Buat objek URI dari URL yang sudah di-trim spasinya
            URI uri = URI.create(url.trim());

            // Ambil host dari URI (contoh: www.example.com)
            String host = uri.getHost();

            // Jika host null atau kosong, kembalikan string kosong
            if (host == null || host.isEmpty()) {
                return "";
            }

            // Ubah host ke huruf kecil lalu konversi ke ASCII (in case domain internasional / IDN)
            return IDN.toASCII(host.toLowerCase());
        } catch (IllegalArgumentException e) {
            // Jika URL tidak valid dan gagal diparse oleh URI, kembalikan string kosong sebagai tanda gagal
            return "";
        }
    }

    /**
     * Melakukan normalisasi URL.
     *
     * @param rawUrl   : URL mentah dari input pengguna
     * @param isStrict : menentukan apakah skema dan host wajib ada
     * @return tiga kemungkinan return:
     * - URL yang sudah dinormalisasi
     * - null jika tidak valid
     * - URL asli jika terjadi error parsing
     */
    public static String normalizeUrl(String rawUrl, boolean isStrict) {
        // Jika URL null atau hanya berisi spasi, langsung anggap tidak valid
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return null;
        }

        try {
            // Buat objek URI dari URL mentah
            URI uri = new URI(rawUrl.trim());

            // Ambil masing-masing komponen URI
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getRawPath();
            String query = uri.getRawQuery();
            int port = uri.getPort();

            // Jika strict mode aktif, skema dan host wajib ada
            if (isStrict) {
                if (scheme == null || scheme.isEmpty()) return null;
                if (host == null || host.isEmpty()) return null;
            }

            // Hanya izinkan skema http atau https
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                return null;
            }

            // Jika port adalah port default (80 untuk http, 443 untuk https), hapus port dengan mengatur nilainya ke -1
            if ((scheme.equalsIgnoreCase("http") && port == 80) || (scheme.equalsIgnoreCase("https") && port == 443)) {
                port = -1;
            }

            // Normalisasi path (hilangkan dot-segment dan double slash)
            path = normalizePath(path);

            // Bangun ulang URI yang sudah dibersihkan
            URI cleaned = new URI(scheme.toLowerCase(), // SCHEME: selalu lowercase
                    null,                 // USERINFO: diabaikan
                    host.toLowerCase(),   // HOST: selalu lowercase
                    port,                 // PORT: sudah disesuaikan
                    path,                 // PATH: hasil normalisasi
                    query,                // QUERY: dipertahankan
                    null                  // FRAGMENT: dibuang
            );

            // Kembalikan URL dalam bentuk ASCII string (in case domain internasional / IDN)
            return cleaned.toASCIIString();
        } catch (Exception e) {
            // Jika terjadi error parsing atau pembuatan URI, kembalikan URL asli agar tetap bisa diproses lebih lanjut
            return rawUrl;
        }
    }

    /**
     * Method untuk melakukan normalisasi path URL.
     *
     * Normalisasi meliputi:
     * - penghapusan segmen ""."" (current directory)
     * - pengolahan segmen "".."" (parent directory)
     * - penghapusan slash "/" berlebih
     *
     * @param path path URL mentah
     * @return path yang sudah dinormalisasi
     */
    private static String normalizePath(String path) {
        // Jika path null atau kosong, default-kan ke root "/"
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // Deque digunakan sebagai stack untuk menyimpan segmen path
        Deque<String> segments = new ArrayDeque<>();

        // Pisahkan path berdasarkan tanda "/"
        for (String part : path.split("/")) {
            // Jika segmen kosong atau ".", abaikan
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            // Jika segmen "..", hapus segmen terakhir (jika ada)
            else if (part.equals("..")) {
                if (!segments.isEmpty()) {
                    segments.removeLast();
                }
            }
            // Jika segmen valid, simpan ke stack
            else {
                segments.add(part);
            }
        }

        StringBuilder sb = new StringBuilder();

        // Bangun kembali path dari segmen yang tersisa
        for (String seg : segments) {
            sb.append("/").append(seg);
        }

        // Jika tidak ada segmen, kembalikan root "/"
        return sb.isEmpty() ? "/" : sb.toString();
    }
}
