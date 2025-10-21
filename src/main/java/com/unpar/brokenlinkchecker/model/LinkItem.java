package com.unpar.brokenlinkchecker.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Representasi satu baris data dalam tabel Result.
 */
public class LinkItem {

    private final StringProperty status;
    private final StringProperty url;

    public LinkItem(String status, String url) {
        this.status = new SimpleStringProperty(status);
        this.url = new SimpleStringProperty(url);
    }

    public StringProperty statusProperty() {
        return status;
    }

    public StringProperty urlProperty() {
        return url;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String value) {
        status.set(value);
    }

    public String getUrl() {
        return url.get();
    }

    public void setUrl(String value) {
        url.set(value);
    }
}
