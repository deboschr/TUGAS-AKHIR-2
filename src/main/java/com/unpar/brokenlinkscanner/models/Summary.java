package com.unpar.brokenlinkscanner.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;

public class Summary {
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.IDLE);
    private final IntegerProperty allLinksCount = new SimpleIntegerProperty(0);
    private final IntegerProperty webpageLinksCount = new SimpleIntegerProperty(0);
    private final IntegerProperty brokenLinksCount = new SimpleIntegerProperty(0);

    private final LongProperty startTime = new SimpleLongProperty(0);
    private final LongProperty endTime = new SimpleLongProperty(0);

    // ===============================================================================
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
    public int getAllLinksCount() {
        return allLinksCount.get();
    }

    public void setAllLinksCount(int value) {
        this.allLinksCount.set(value);
    }

    @SuppressWarnings("exports")
    public IntegerProperty allLinksCountProperty() {
        return allLinksCount;
    }

    // ===============================================================================
    public int getWebpages() {
        return webpageLinksCount.get();
    }

    public void setWebpages(int value) {
        this.webpageLinksCount.set(value);
    }

    @SuppressWarnings("exports")
    public IntegerProperty webpageLinksCountProperty() {
        return webpageLinksCount;
    }

    // ===============================================================================
    public int getBrokenLinksCount() {
        return brokenLinksCount.get();
    }

    public void setBrokenLinksCount(int value) {
        this.brokenLinksCount.set(value);
    }

    @SuppressWarnings("exports")
    public IntegerProperty brokenLinksCountProperty() {
        return brokenLinksCount;
    }

    // ===============================================================================
    public long getStartTime() {
        return startTime.get();
    }

    public void setStartTime(long value) {
        this.startTime.set(value);
    }

    public LongProperty startTimeProperty() {
        return startTime;
    }

    // ===============================================================================
    public long getEndTime() {
        return endTime.get();
    }

    public void setEndTime(long value) {
        this.endTime.set(value);
    }

    public LongProperty endTimeProperty() {
        return endTime;
    }
}
