package com.unpar.brokenlinkscanner.models;

import com.unpar.brokenlinkscanner.utils.HttpHandler;
import javafx.beans.property.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Link {
    // URL awal
    private final StringProperty url;
    // URL hasil redirect
    private final StringProperty finalUrl = new SimpleStringProperty("");
    // Kode status HTTP
    private final IntegerProperty statusCode = new SimpleIntegerProperty(0);
    // Tipe konten yang menjadi response body HTTP
    private final StringProperty contentType = new SimpleStringProperty("");
    // Pesan error connection atau ststus code + reason phrase
    private final StringProperty error = new SimpleStringProperty("");

    /**
     * Untuk menandakan apakah link ini adalah link webpage atau bukan.
     * Syarat sebuah link menjadi webpage link:
     * - host-nya harus sama dengan host dari seed URL
     * - response body-nya harus HTML
     * - tidak error
     */
    private final BooleanProperty isWebpage = new SimpleBooleanProperty(false);

    /**
     * Menyimpan relasi antar Link:
     * - key = Link lain yang terhubung dengan link ini
     * - value = anchor text yaitu teks yang ada di dalam tag a HTML
     */
    private final Map<Link, String> connections = new ConcurrentHashMap<>();

    public Link(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        this.url = new SimpleStringProperty(url);
    }

    // ===============================================================================
    // URL

    public String getUrl() {
        return url.get();
    }

    public void setUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        url.set(value);
    }

    public StringProperty urlProperty() {
        return url;
    }

    // ===============================================================================
    // Final URL

    public String getFinalUrl() {
        return finalUrl.get();
    }

    public void setFinalUrl(String value) {
        finalUrl.set(value != null ? value : "");
    }

    public StringProperty finalUrlProperty() {
        return finalUrl;
    }

    // ===============================================================================
    // Status Code

    public Integer getStatusCode() {
        return statusCode.get();
    }

    public void setStatusCode(int value) {
        // Kalau status kode termasuk error, set juga pesan error-nya
        String status = HttpHandler.getStatusError(value);
        if (status != null) {
            error.set(status);
        }
        statusCode.set(value);
    }

    public IntegerProperty statusProperty() {
        return statusCode;
    }

    // ===============================================================================
    // Content Type

    public String getContentType() {
        return contentType.get();
    }

    public void setContentType(String value) {
        contentType.set(value != null ? value : "");
    }

    public StringProperty contentTypeProperty() {
        return contentType;
    }

    // ===============================================================================
    // Error

    public String getError() {
        return error.get();
    }

    public void setError(String value) {
        error.set(value != null ? value : "");
    }

    public StringProperty errorProperty() {
        return error;
    }

    // ===============================================================================
    // isWebpage

    public boolean isWebpage() {
        return isWebpage.get();
    }

    public void setIsWebpage(boolean value) {
        isWebpage.set(value);
    }

    public BooleanProperty isWebpageProperty() {
        return isWebpage;
    }

    // ===============================================================================
    // Relasi antar link (graph koneksi)

    /**
     * Menambahkan relasi (koneksi) antar dua Link.
     * Koneksi dibuat dua arah:
     * - this ke other
     * - other ke this
     *
     * @param other      link lain yang terhubung
     * @param anchorText teks yang menghubungkan (boleh null, akan diset "")
     */
    public void addConnection(Link other, String anchorText) {
        if (other == null || other == this) {
            return;
        }

        // Tambahkan koneksi dua arah
        this.connections.putIfAbsent(other, anchorText != null ? anchorText : "");
        other.connections.putIfAbsent(this, anchorText != null ? anchorText : "");
    }

    public Map<Link, String> getConnection() {
        return connections;
    }
}
