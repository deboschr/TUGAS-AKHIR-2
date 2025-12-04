package com.unpar.brokenlinkscanner.utils;

import java.net.IDN;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;

public class UrlHandler {

    public static String getHost(String url) {
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();

            if (host == null || host.isEmpty()) {
                return "";
            }

            return IDN.toASCII(host.toLowerCase());
        } catch (IllegalArgumentException e) {
            return "";
        }
    }


    public static String normalizeUrl(String rawUrl, boolean isStrict) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return null;
        }

        try {

            URI uri = new URI(rawUrl.trim());

            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getRawPath();
            String query = uri.getRawQuery();
            int port = uri.getPort();


            if (isStrict) {
                if (scheme == null || scheme.isEmpty()) return null;
                if (host == null || host.isEmpty()) return null;
            }

            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                return null;
            }

            if ((scheme.equalsIgnoreCase("http") && port == 80) || (scheme.equalsIgnoreCase("https") && port == 443)) {
                port = -1;
            }

            path = normalizePath(path);

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
            return rawUrl;
        }
    }

    public static String normalizePath(String path) {

        if (path == null || path.isEmpty()) {
            return "/";
        }

        Deque<String> segments = new ArrayDeque<>();

        for (String part : path.split("/")) {

            if (part.isEmpty() || part.equals(".")) {
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

}
