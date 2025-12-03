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

    private final Map<Link, String> relations = new ConcurrentHashMap<>();

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

    public void setUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        url.set(value);
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
    public void addRelation(Link other, String anchorText) {
        if (other == null || other == this) {
            return;
        }

        this.relations.putIfAbsent(other, anchorText != null ? anchorText : "");
        other.relations.putIfAbsent(this, anchorText != null ? anchorText : "");
    }

    public Map<Link, String> getRelation() {
        return relations;
    }
}
