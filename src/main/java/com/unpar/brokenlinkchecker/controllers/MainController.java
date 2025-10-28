package com.unpar.brokenlinkchecker.controllers;

import com.unpar.brokenlinkchecker.cores.Crawler;
import com.unpar.brokenlinkchecker.models.CheckingStatus;
import com.unpar.brokenlinkchecker.models.Link;
import com.unpar.brokenlinkchecker.models.Summary;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;

public class MainController {
    // ============================= FXML =============================
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

    private final ObservableList<Link> allLinks = FXCollections.observableArrayList();

    private final Summary summaryCard = new Summary();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            initTitleBar();
            initResultTable();
            initSummaryCard();
            initButtonState();

            crawler = new Crawler(link -> allLinks.add(link));
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

        // Kosongkan data lama
        allLinks.clear();
        // Update status jadi checking
        summaryCard.setCheckingStatus(CheckingStatus.CHECKING);

        // jalanin di thread background
        new Thread(() -> {
            // Jalankan crawler di backgroud thread
            crawler.start(cleanedSeedUrl);

            // Kalau proses crawling udah beres secara alami, update status jadi completed
            if (!crawler.isStopped()) {
                Platform.runLater(() -> summaryCard.setCheckingStatus(CheckingStatus.COMPLETED));
            }
        }).start();
    }

    @FXML
    private void onStopClick() {
        if (crawler != null) {
            crawler.stop();
            summaryCard.setCheckingStatus(CheckingStatus.STOPPED);
        }
    }

    @FXML
    private void onExportClick() {
        showAlert("Export not implemented yet.");
    }

    // ============================= TITLE BAR =============================
    private void initTitleBar() {
        Stage stage = (Stage) titleBar.getScene().getWindow();

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
        summaryCard.checkingStatusProperty().addListener((obs, old, status) -> {
            switch (status) {
                case IDLE -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    exportButton.setDisable(true);

                    startBtn.getStyleClass().remove("btn-start-active");
                    stopBtn.getStyleClass().remove("btn-stop-active");
                }
                case CHECKING -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(false);
                    exportButton.setDisable(true);

                    stopBtn.getStyleClass().remove("btn-stop-active");

                    if (!startBtn.getStyleClass().contains("btn-start-active")) {
                        startBtn.getStyleClass().add("btn-start-active");
                    }
                }
                case STOPPED -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(false);
                    exportButton.setDisable(false);

                    startBtn.getStyleClass().remove("btn-start-active");

                    if (!stopBtn.getStyleClass().contains("btn-stop-active")) {
                        stopBtn.getStyleClass().add("btn-stop-active");
                    }
                }
                case COMPLETED -> {
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

        // Label mengikuti nilai di Summary
        checkingStatusLabel.textProperty().bind(summaryCard.checkingStatusProperty().asString());
        totalLinksLabel.textProperty().bind(summaryCard.totalLinksProperty().asString());
        webpageLinksLabel.textProperty().bind(summaryCard.webpageLinksProperty().asString());
        brokenLinksLabel.textProperty().bind(summaryCard.brokenLinksProperty().asString());

        // Total links: langsung binding ke ukuran allLinks
        summaryCard.totalLinksProperty().bind(Bindings.size(allLinks));
        // Webpage links: hitung berapa banyak link di allLinks yang isWebpage == true
        summaryCard.webpageLinksProperty().bind(Bindings.createIntegerBinding(() ->
                (int) allLinks.stream().filter(Link::isWebpage).count(), allLinks
        ));

        // Broken links: hitung link yang error-nya tidak kosong
        summaryCard.brokenLinksProperty().bind(Bindings.createIntegerBinding(() ->
                (int) allLinks.stream()
                        .filter(link -> !link.getError().isEmpty())
                        .count(), allLinks
        ));

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

        // Filter hanya link rusak dari allLinks
        FilteredList<Link> brokenOnly = new FilteredList<>(allLinks, link ->
                !link.getError().isEmpty()
        );

        // Set ke tabel
        resultTable.setItems(brokenOnly);

        // Kalau baris di klik maka akan buka jendela baru
        resultTable.setRowFactory(tv -> {
            TableRow<Link> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    Link selected = row.getItem();
                    showLinkDetailWindow(selected);
                }
            });
            return row;
        });


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

    private void initTableFilter() {
    }

    // ============================= UTILS =============================
    private void showLinkDetailWindow(Link link) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/unpar/brokenlinkchecker/link_detail.fxml"));
            Parent root = loader.load();

            LinkDetailController controller = loader.getController();
            controller.setLink(link);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle("Broken Link Detail");
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to open detail window: " + e.getMessage());
        }
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
     * valid
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
