package com.unpar.brokenlinkscanner.models;

import com.unpar.brokenlinkscanner.utils.HttpHandler;
import javafx.beans.property.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Link {

    private final StringProperty url;

    private final StringProperty finalUrl = new SimpleStringProperty("");

    private final IntegerProperty statusCode = new SimpleIntegerProperty(0);

    private final StringProperty contentType = new SimpleStringProperty("");

    private final StringProperty error = new SimpleStringProperty("");

    private final BooleanProperty isWebpage = new SimpleBooleanProperty(false);

    private final Map<Link, String> webpageSources = new ConcurrentHashMap<>();

    public Link(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        this.url = new SimpleStringProperty(url);
    }


    // =============================================================================
    public String getUrl() {
        return url.get();
    }


    public StringProperty urlProperty() {
        return url;
    }


    // =============================================================================
    public String getFinalUrl() {
        return finalUrl.get();
    }

    public void setFinalUrl(String value) {
        finalUrl.set(value != null ? value : "");
    }

    public StringProperty finalUrlProperty() {
        return finalUrl;
    }

    // =============================================================================
    public Integer getStatusCode() {
        return statusCode.get();
    }

    public void setStatusCode(int value) {

        String status = HttpHandler.getStatusError(value);
        if (status != null) {
            error.set(status);
        }
        statusCode.set(value);
    }

    public IntegerProperty statusProperty() {
        return statusCode;
    }


    // =============================================================================

    public String getContentType() {
        return contentType.get();
    }

    public void setContentType(String value) {
        contentType.set(value != null ? value : "");
    }

    public StringProperty contentTypeProperty() {
        return contentType;
    }

    // =============================================================================
    public String getError() {
        return error.get();
    }

    public void setError(String value) {
        error.set(value != null ? value : "");
    }

    public StringProperty errorProperty() {
        return error;
    }

    // =============================================================================
    public boolean isWebpage() {
        return isWebpage.get();
    }

    public void setIsWebpage(boolean value) {
        isWebpage.set(value);
    }

    public BooleanProperty isWebpageProperty() {
        return isWebpage;
    }


    // =============================================================================
    public void addWebpageSource(Link webpageLink, String anchorText) {
        if (webpageLink == null || webpageLink == this) {
            return;
        }

        this.webpageSources.putIfAbsent(webpageLink, anchorText != null ? anchorText : "");
    }

    public Map<Link, String> getWebpageSources() {
        return webpageSources;
    }

    // =============================================================================
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof Link)) return false;

        Link other = (Link) obj;

        return this.getUrl().equals(other.getUrl());
    }

    @Override
    public int hashCode() {
        return getUrl().hashCode();
    }

}
