package com.unpar.brokenlinkchecker.model;

import javafx.beans.property.*;
import java.util.HashMap;
import java.util.Map;

import com.unpar.brokenlinkchecker.util.HttpStatus;

public class Link {

   private final StringProperty url;
   private final StringProperty finalUrl;
   private final IntegerProperty statusCode;
   private final StringProperty contentType;
   private final StringProperty error;

   private final Map<Link, String> connections;

   public Link(String url, String finalUrl, Integer statusCode, String contentType, String error) {
      this.url = new SimpleStringProperty(url);
      this.finalUrl = new SimpleStringProperty(finalUrl);
      this.statusCode = new SimpleIntegerProperty(statusCode);
      this.contentType = new SimpleStringProperty(contentType);
      this.error = new SimpleStringProperty((error != null)
            ? error
            : HttpStatus.getStatus(statusCode));

      this.connections = new HashMap<>();
   }

   // ===============================================================================
   // URL
   public String getUrl() {
      return url.get();
   }

   public void setUrl(String value) {
      url.set(value);
   }

   @SuppressWarnings("exports")
   public StringProperty urlProperty() {
      return url;
   }

   // ===============================================================================
   // Final URL
   public String getFinalUrl() {
      return finalUrl.get();
   }

   public void setFinalUrl(String value) {
      finalUrl.set(value);
   }

   @SuppressWarnings("exports")
   public StringProperty finalUrlProperty() {
      return finalUrl;
   }

   // ===============================================================================
   // Status Code
   public Integer getStatusCode() {
      return statusCode.get();
   }

   public void setStatusCode(int value) {
      statusCode.set(value);
   }

   @SuppressWarnings("exports")
   public IntegerProperty statusProperty() {
      return statusCode;
   }

   // ===============================================================================
   // Content Type
   public String getContentType() {
      return contentType.get();
   }

   public void setContentType(String value) {
      contentType.set(value);
   }

   @SuppressWarnings("exports")
   public StringProperty contentTypeProperty() {
      return contentType;
   }

   // ===============================================================================
   // Error
   public String getError() {
      return error.get();
   }

   public void setError(String value) {
      error.set(value);
   }

   @SuppressWarnings("exports")
   public StringProperty errorProperty() {
      return error;
   }

   // ===============================================================================
   // Relasi antar link

   public void setConnection(Link other, String anchorText) {
      if (other == null || other == this)
         return;

      // Tambahkan koneksi dua arah
      this.connections.putIfAbsent(other, anchorText != null ? anchorText : "");
      other.connections.putIfAbsent(this, anchorText != null ? anchorText : "");
   }

   public Map<Link, String> getConnection() {
      return connections;
   }

   public void clearConnection() {
      this.connections.clear();
   }

}
