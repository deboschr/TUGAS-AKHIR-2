package com.unpar.brokenlinkchecker.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class SummaryCard {
   private final ObjectProperty<CheckingStatus> checkingStatus = new SimpleObjectProperty<>(CheckingStatus.IDLE);
   private final IntegerProperty totalLinks = new SimpleIntegerProperty(0);
   private final IntegerProperty webpages = new SimpleIntegerProperty(0);
   private final IntegerProperty brokenLinks = new SimpleIntegerProperty(0);

   // ===============================================================================
   // CheckingStatus
   public CheckingStatus getCheckingStatus() {
      return checkingStatus.get();
   }

   public void setCheckingStatus(CheckingStatus value) {
      this.checkingStatus.set(value);
   }

   public ObjectProperty<CheckingStatus> checkingStatusProperty() {
      return checkingStatus;
   }

   // ===============================================================================
   // TotalLinks
   public int getTotalLinks() {
      return totalLinks.get();
   }

   public void setTotalLinks(int value) {
      this.totalLinks.set(value);
   }

   public IntegerProperty totalLinksProperty() {
      return totalLinks;
   }

   // ===============================================================================
   // Webpages

   public int getWebpages() {
      return webpages.get();
   }

   public void setWebpages(int value) {
      this.webpages.set(value);
   }

   public IntegerProperty webpagesProperty() {
      return webpages;
   }

   // ===============================================================================
   // BrokenLinks

   public int getBrokenLinks() {
      return brokenLinks.get();
   }

   public void setBrokenLinks(int value) {
      this.brokenLinks.set(value);
   }

   public IntegerProperty brokenLinksProperty() {
      return brokenLinks;
   }
}
