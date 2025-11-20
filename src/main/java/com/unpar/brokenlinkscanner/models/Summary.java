package com.unpar.brokenlinkscanner.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class Summary {
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.IDLE);
    private final IntegerProperty totalLinks = new SimpleIntegerProperty(0);
    private final IntegerProperty webpageLinks = new SimpleIntegerProperty(0);
    private final IntegerProperty brokenLinks = new SimpleIntegerProperty(0);

    // ===============================================================================
    // Status
    public Status getStatus() {
        return status.get();
    }

    public void setStatus(Status value) {
        this.status.set(value);
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    // ===============================================================================
    // TotalLinks
    public int getTotalLinks() {
        return totalLinks.get();
    }

    public void setTotalLinks(int value) {
        this.totalLinks.set(value);
    }

    @SuppressWarnings("exports")
    public IntegerProperty totalLinksProperty() {
        return totalLinks;
    }

    // ===============================================================================
    // Webpages

    public int getWebpages() {
        return webpageLinks.get();
    }

    public void setWebpages(int value) {
        this.webpageLinks.set(value);
    }

    @SuppressWarnings("exports")
    public IntegerProperty webpageLinksProperty() {
        return webpageLinks;
    }

    // ===============================================================================
    // BrokenLinks

    public int getBrokenLinks() {
        return brokenLinks.get();
    }

    public void setBrokenLinks(int value) {
        this.brokenLinks.set(value);
    }

    @SuppressWarnings("exports")
    public IntegerProperty brokenLinksProperty() {
        return brokenLinks;
    }
}
