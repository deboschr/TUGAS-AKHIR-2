package com.unpar.brokenlinkchecker.models;

import com.unpar.brokenlinkchecker.utils.HttpHandler;
import javafx.beans.property.*;
import java.util.HashMap;
import java.util.Map;

public class Link {

   private final StringProperty url; // URL utama
   private final StringProperty finalUrl; // URL hasil redirect
   private final IntegerProperty statusCode; // Kode status HTTP
   private final StringProperty contentType; // Tipe konten
   private final StringProperty error; // Pesan error
   private final BooleanProperty isWebpage; // Buat nentuin apakah halaman atau bukan
   private final Map<Link, String> connections; // Hubungan antar link + anchor text


   public Link(String url) {

      if (url == null || url.isBlank()) {
         throw new IllegalArgumentException("URL tidak boleh null atau kosong");
      }

      this.url = new SimpleStringProperty(url);
      this.connections = new HashMap<>();

      this.statusCode = new SimpleIntegerProperty(0);
      this.contentType = new SimpleStringProperty("");
      this.finalUrl = new SimpleStringProperty("");
      this.error = new SimpleStringProperty("");
      this.isWebpage = new SimpleBooleanProperty(false);
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
      String status = HttpHandler.getErrorStatus(value);
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

   // =======================================================================
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
   // Relasi antar link

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
