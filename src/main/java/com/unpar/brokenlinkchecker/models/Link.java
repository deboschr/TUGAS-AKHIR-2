package com.unpar.brokenlinkchecker.models;

import com.unpar.brokenlinkchecker.utils.HttpStatus;
import javafx.beans.property.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Link {

    private final StringProperty url; // URL utama
    private final StringProperty finalUrl; // URL hasil redirect (kalau ada)
    private final IntegerProperty statusCode; // Kode status HTTP
    private final StringProperty contentType; // Tipe konten (Content-Type)
    private final StringProperty error; // Pesan error / reason phrase
    private final BooleanProperty isWebpage; // Menandai apakah link ini adalah webpage same-host

    /**
     * Menyimpan relasi antar Link:
     * - key = Link lain yang terhubung dengan link ini
     * - value = anchor text (teks di dalam tag a HTML) yang menghubungkan keduanya
     */
    private final Map<Link, String> connections;

    public Link(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL tidak boleh null atau kosong");
        }

        this.url = new SimpleStringProperty(url);
        this.finalUrl = new SimpleStringProperty("");
        this.statusCode = new SimpleIntegerProperty(0);
        this.contentType = new SimpleStringProperty("");
        this.error = new SimpleStringProperty("");
        this.isWebpage = new SimpleBooleanProperty(false);

        // Pake ConcurrentHashMap biar aman untuk operasi multithread
        this.connections = new ConcurrentHashMap<>();
    }

    // ===============================================================================
    // URL

    public String getUrl() {
        return url.get();
    }

    public void setUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("URL tidak boleh null atau kosong");
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
        String status = HttpStatus.getStatusError(value);
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
     * @param anchorText teks anchor yang menghubungkan (boleh null, akan diset "")
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
