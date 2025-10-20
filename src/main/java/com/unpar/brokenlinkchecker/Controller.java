package com.unpar.brokenlinkchecker;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;

import com.unpar.brokenlinkchecker.model.*;

public class Controller {

   // ============================= FXML =============================
   @FXML
   private BorderPane root;

   // Title bar
   @FXML
   private HBox titleBar;
   @FXML
   private Button minimizeBtn, maximizeBtn, closeBtn;

   // Input + Control Button
   @FXML
   private TextField seedUrlField;
   @FXML
   private Button startBtn, stopBtn, exportButton;

   // Summary
   @FXML
   private Label checkingStatusLabel, totalLinksLabel, webpageLinksLabel, brokenLinksLabel;

   // Filters
   @FXML
   private ComboBox<String> urlFilterOption, statusCodeFilterOption;
   @FXML
   private TextField urlFilterField, statusCodeFilterField;

   // Result Table
   @FXML
   private TableView<Link> resultTable;
   @FXML
   private TableColumn<Link, String> statusColumn, urlColumn;

   // ============================= FIELDS =============================
   private Crawler crawler;

   private double xOffset = 0;
   private double yOffset = 0;

   // ObservableList untuk ditampilkan ke GUI
   private final ObservableList<String> totalLinks = FXCollections.observableArrayList();
   private final ObservableList<Link> webpageLinks = FXCollections.observableArrayList();
   private final ObservableList<Link> brokenLinks = FXCollections.observableArrayList();

   // Model summary card yang akan di-bind ke label
   private final SummaryCard summaryCard = new SummaryCard();

   @FXML
   public void initialize() {
      Platform.runLater(() -> {
         initTitleBar();
         initResultTable();
         initSummaryCard();
         initButtonState();
         
         crawler = new Crawler(
               checkingStatus -> summaryCard.setCheckingStatus(checkingStatus),
               totalUrl -> totalLinks.add(totalUrl),
               webpage -> webpageLinks.add(webpage),
               broken -> brokenLinks.add(broken));

      });
   }

   // ============================= EVENT HANDLERS =============================
   @FXML
   private void onStartClick() {
      String seedUrl = seedUrlField.getText().trim();

      String cleanedSeedUrl = validateSeedUrl(seedUrl);

      if (cleanedSeedUrl == null) {
         showAlert("Invalid URL. Please enter a valid URL.");
         return;
      }

      // Warna tombol aktif
      if (!startBtn.getStyleClass().contains("btn-start-active")) {
         startBtn.getStyleClass().add("btn-start-active");
      }

      // kosongkan data lama
      totalLinks.clear();
      webpageLinks.clear();
      brokenLinks.clear();

      // jalanin di thread background
      new Thread(() -> crawler.start(cleanedSeedUrl)).start();
   }

   @FXML
   private void onStopClick() {
      if (crawler != null) {

         crawler.stop();
      }
   }

   @FXML
   private void onExportClick() {
      showAlert("Export not implemented yet.");
   }

   // ============================= TITLE BAR =============================
   private void initTitleBar() {
      Stage stage = (Stage) titleBar.getScene().getWindow();

      // biar bisa digeser manual
      titleBar.setOnMousePressed((MouseEvent e) -> {
         xOffset = e.getSceneX();
         yOffset = e.getSceneY();
      });

      titleBar.setOnMouseDragged((MouseEvent e) -> {
         stage.setX(e.getScreenX() - xOffset);
         stage.setY(e.getScreenY() - yOffset);
      });

      minimizeBtn.setOnAction(e -> stage.setIconified(true));
      maximizeBtn.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));
      closeBtn.setOnAction(e -> stage.close());
   }

   // ============================= BUTTON STATE & STYLE =============================
   private void initButtonState() {
      // kondisi awal
      stopBtn.setDisable(true);
      exportButton.setDisable(true);

      // hapus warna aktif dari start/stop di awal
      startBtn.getStyleClass().remove("btn-start-active");
      stopBtn.getStyleClass().remove("btn-stop-active");

      // dengarkan perubahan status
      summaryCard.checkingStatusProperty().addListener((obs, old, status) -> {
         switch (status) {
            case IDLE -> {
               // start selalu aktif, stop/export nonaktif
               startBtn.setDisable(false);
               stopBtn.setDisable(true);
               exportButton.setDisable(true);

               startBtn.getStyleClass().remove("btn-start-active");
               stopBtn.getStyleClass().remove("btn-stop-active");
            }
            case CHECKING -> {
               // stop aktif, export nonaktif
               startBtn.setDisable(false);
               stopBtn.setDisable(false);
               exportButton.setDisable(true);

               stopBtn.getStyleClass().remove("btn-stop-active");
            }
            case STOPPED -> {
               // stop nonaktif, export aktif
               startBtn.setDisable(false);
               stopBtn.setDisable(true);
               exportButton.setDisable(false);

               // hapus semua warna aktif
               startBtn.getStyleClass().remove("btn-start-active");
               stopBtn.getStyleClass().remove("btn-stop-active");

                        if (!stopBtn.getStyleClass().contains("btn-stop-active")) {
            stopBtn.getStyleClass().add("btn-stop-active");
         }
            }
            case COMPLETED -> {
               // stop nonaktif, export aktif
               startBtn.setDisable(false);
               stopBtn.setDisable(true);
               exportButton.setDisable(false);

               // hapus semua warna aktif
               startBtn.getStyleClass().remove("btn-start-active");
               stopBtn.getStyleClass().remove("btn-stop-active");
            }
         }
      });
   }

   // ============================= SUMMARY CARD =============================
   private void initSummaryCard() {

      // Label mengikuti nilai di SummaryCard
      checkingStatusLabel.textProperty().bind(summaryCard.checkingStatusProperty().asString());
      totalLinksLabel.textProperty().bind(summaryCard.totalLinksProperty().asString());
      webpageLinksLabel.textProperty().bind(summaryCard.webpagesProperty().asString());
      brokenLinksLabel.textProperty().bind(summaryCard.brokenLinksProperty().asString());

      // SummaryCard mengikuti ukuran ObservableList
      summaryCard.totalLinksProperty().bind(Bindings.size(totalLinks));
      summaryCard.webpagesProperty().bind(Bindings.size(webpageLinks));
      summaryCard.brokenLinksProperty().bind(Bindings.size(brokenLinks));

      // Warna dinamis berdasarkan status
      summaryCard.checkingStatusProperty().addListener((obs, old, status) -> {
         switch (status) {
            case IDLE -> checkingStatusLabel.setStyle("-fx-text-fill: #f9fafb;"); // putih
            case CHECKING -> checkingStatusLabel.setStyle("-fx-text-fill: #60a5fa;"); // biru
            case STOPPED -> checkingStatusLabel.setStyle("-fx-text-fill: #ef4444;"); // merah
            case COMPLETED -> checkingStatusLabel.setStyle("-fx-text-fill: #10b981;"); // hijau
         }
      });
   }

   // ============================= RESULT TABLE =============================
   private void initResultTable() {
      // Atur lebar kolom
      statusColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.2));
      urlColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.8));

      // Sumber data
      statusColumn.setCellValueFactory(cell -> cell.getValue().errorProperty());
      urlColumn.setCellValueFactory(cell -> cell.getValue().urlProperty());

      // Nonaktifkan resize manual kolom
      resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

      // Bungkus ObservableList dengan FilteredList
      FilteredList<Link> filteredLinks = new FilteredList<>(brokenLinks, p -> true);

      // Inisialisasi sistem filter
      initTableFilter(filteredLinks);

      // Set ke tabel
      resultTable.setItems(filteredLinks);

      // STATUS COLUMN — teks berwarna
      statusColumn.setCellFactory(col -> new TableCell<>() {
         @Override
         protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
               setText(null);
               setStyle("");
            } else {
               Link link = getTableView().getItems().get(getIndex());
               int code = link.getStatusCode();
               setText(status);

               // warna merah untuk error
               if (code >= 400 && code < 600)
                  setStyle("-fx-text-fill: #ef4444;");
               else
                  setStyle("-fx-text-fill: #f9fafb;");
            }
         }
      });

      // URL COLUMN — hyperlink klik-buka di browser
      urlColumn.setCellFactory(col -> new TableCell<>() {
         private final Hyperlink link = new Hyperlink();

         {
            link.setOnAction(e -> {
               String url = link.getText();
               try {
                  if (Desktop.isDesktopSupported()) {
                     Desktop.getDesktop().browse(new URI(url));
                  }
               } catch (Exception ex) {
                  ex.printStackTrace();
               }
            });
         }

         @Override
         protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
               setGraphic(null);
            } else {
               link.setText(item);
               link.setStyle("-fx-text-fill: #60a5fa; -fx-underline: true;");
               setGraphic(link);
            }
         }
      });
   }

   private void initTableFilter(FilteredList<Link> filteredLinks) {
      Runnable updateFilter = () -> {
         String urlOption = urlFilterOption.getValue();
         String urlText = urlFilterField.getText();
         String statusOption = statusCodeFilterOption.getValue();
         String statusText = statusCodeFilterField.getText();

         // Jika semua kosong → tampilkan semua data
         if ((urlOption == null || urlOption.isBlank() || urlText == null || urlText.isBlank()) &&
               (statusOption == null || statusOption.isBlank() || statusText == null || statusText.isBlank())) {
            filteredLinks.setPredicate(p -> true);
            return;
         }

         filteredLinks.setPredicate(link -> {
            boolean urlMatches = true;
            boolean statusMatches = true;

            // ===== URL filter =====
            if (urlOption != null && !urlOption.isBlank() &&
                  urlText != null && !urlText.isBlank()) {

               String url = link.getUrl().toLowerCase();
               String keyword = urlText.toLowerCase();

               switch (urlOption) {
                  case "Equals" -> urlMatches = url.equals(keyword);
                  case "Contains" -> urlMatches = url.contains(keyword);
                  case "Starts With" -> urlMatches = url.startsWith(keyword);
                  case "Ends With" -> urlMatches = url.endsWith(keyword);
               }
            }

            // ===== Status Code filter =====
            if (statusOption != null && !statusOption.isBlank() &&
                  statusText != null && !statusText.isBlank()) {

               try {
                  int code = link.getStatusCode();
                  int filterValue = Integer.parseInt(statusText);

                  switch (statusOption) {
                     case "Equals" -> statusMatches = code == filterValue;
                     case "Greater Than" -> statusMatches = code > filterValue;
                     case "Less Than" -> statusMatches = code < filterValue;
                  }
               } catch (NumberFormatException e) {
                  statusMatches = false; // Input bukan angka
               }
            }

            return urlMatches && statusMatches; // AND logic
         });
      };

      // Listener untuk semua input filter
      urlFilterOption.valueProperty().addListener((obs, o, n) -> updateFilter.run());
      urlFilterField.textProperty().addListener((obs, o, n) -> updateFilter.run());
      statusCodeFilterOption.valueProperty().addListener((obs, o, n) -> updateFilter.run());
      statusCodeFilterField.textProperty().addListener((obs, o, n) -> updateFilter.run());
   }

   // ============================= UTILS =============================
   private void showAlert(String message) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
   }

   /**
    * Validasi dan normalisasi seed URL.
    * 
    * Aturan:
    * 1. Wajib punya scheme (http / https)
    * 2. Wajib punya host
    * 3. Hapus port jika default (80 untuk http, 443 untuk https)
    * 4. Bersihkan path dari dot-segment (., ..)
    * 5. Hapus fragment (#...)
    * 
    * @param rawUrl input mentah dari TextField
    * @return URL yang sudah divalidasi dan dinormalisasi, atau null jika tidak
    *         valid
    */
   private String validateSeedUrl(String rawUrl) {
      if (rawUrl == null || rawUrl.isBlank())
         return null;

      try {
         // tambahkan skema default jika user lupa (misal "example.com" →
         // "http://example.com")
         if (!rawUrl.matches("(?i)^https?://.*")) {
            rawUrl = "http://" + rawUrl.trim();
         }

         URI uri = new URI(rawUrl.trim());

         String scheme = uri.getScheme();
         String host = uri.getHost();
         int port = uri.getPort();
         String path = uri.getRawPath();
         String query = uri.getRawQuery();

         // ===== validasi dasar =====
         if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            return null; // skema tidak valid
         }

         if (host == null || host.isEmpty()) {
            return null; // host wajib ada
         }

         // ===== bersihkan port =====
         if ((scheme.equalsIgnoreCase("http") && port == 80) ||
               (scheme.equalsIgnoreCase("https") && port == 443)) {
            port = -1; // hapus port default
         }

         // ===== bersihkan path (dot segment) =====
         path = normalizePath(path);

         // ===== rakit ulang tanpa fragment =====
         URI cleaned = new URI(
               scheme.toLowerCase(),
               null,
               host.toLowerCase(),
               port,
               path,
               query,
               null // fragment dihapus
         );

         return cleaned.toASCIIString();

      } catch (Exception e) {
         return null; // URL tidak valid
      }
   }

   /**
    * Bersihkan dot-segment (., ..) dari path sesuai RFC 3986 Section 5.2.4
    */
   private String normalizePath(String path) {
      if (path == null || path.isEmpty()) {
         return "/";
      }

      Deque<String> segments = new ArrayDeque<>();

      for (String part : path.split("/")) {
         if (part.equals("") || part.equals(".")) {
            continue;
         } else if (part.equals("..")) {
            if (!segments.isEmpty()) {
               segments.removeLast();
            }
         } else {
            segments.add(part);
         }
      }

      StringBuilder sb = new StringBuilder();

      for (String seg : segments) {
         sb.append("/").append(seg);
      }

      return sb.isEmpty() ? "/" : sb.toString();
   }

}
