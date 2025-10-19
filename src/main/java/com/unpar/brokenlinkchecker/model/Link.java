package com.unpar.brokenlinkchecker.model;

import javafx.beans.property.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.unpar.brokenlinkchecker.util.HttpStatus;

public class Link {

   private final StringProperty url;
   private final StringProperty finalUrl;
   private final IntegerProperty statusCode;
   private final ObjectProperty<Instant> accessTime;
   private final StringProperty contentType;
   private final StringProperty error;

   private final Map<Link, String> connections;

   public Link(String url, String finalUrl, int statusCode, String contentType, String error, Instant accessTime) {
      this.url = new SimpleStringProperty(url);
      this.finalUrl = new SimpleStringProperty(finalUrl);
      this.statusCode = new SimpleIntegerProperty(statusCode);
      this.accessTime = new SimpleObjectProperty<>(accessTime);
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

   public StringProperty finalUrlProperty() {
      return finalUrl;
   }

   // ===============================================================================
   // Status Code
   public int getStatusCode() {
      return statusCode.get();
   }

   public void setStatusCode(int value) {
      statusCode.set(value);
   }

   public IntegerProperty statusProperty() {
      return statusCode;
   }

   // ===============================================================================
   // Access Time
   public Instant getAccessTime() {
      return accessTime.get();
   }

   public void setAccessTime(Instant value) {
      accessTime.set(value);
   }

   public ObjectProperty<Instant> accessTimeProperty() {
      return accessTime;
   }

   // ===============================================================================
   // Content Type
   public String getContentType() {
      return contentType.get();
   }

   public void setContentType(String value) {
      contentType.set(value);
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
      error.set(value);
   }

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
