package com.unpar.brokenlinkchecker.controllers;

import com.unpar.brokenlinkchecker.Application;
import com.unpar.brokenlinkchecker.cores.Crawler;
import com.unpar.brokenlinkchecker.models.CheckingStatus;
import com.unpar.brokenlinkchecker.models.Link;
import com.unpar.brokenlinkchecker.models.Summary;
import com.unpar.brokenlinkchecker.utils.ExportHandler;
import com.unpar.brokenlinkchecker.utils.UrlHandler;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.net.URI;

public class MainController {
    // ======================== GUI Component ========================
    @FXML
    private HBox titleBar, paginationBar;
    @FXML
    private Button minimizeBtn, maximizeBtn, closeBtn, startBtn, stopBtn, exportButton;
    @FXML
    private Label checkingStatusLabel, totalLinksLabel, webpageLinksLabel, brokenLinksLabel;
    @FXML
    private TextField seedUrlField, urlFilterField, statusCodeFilterField;
    @FXML
    private ComboBox<String> urlFilterOption, statusCodeFilterOption;
    @FXML
    private TableView<Link> brokenLinkTable;
    @FXML
    private TableColumn<Link, String> errorColumn, urlColumn;

    // ======================== Data Model ===========================
    private final ObservableList<Link> allLinks = FXCollections.observableArrayList();
    private final Summary summaryCard = new Summary();

    // ======================== Drag Window ==========================
    private double xOffset;
    private double yOffset;

    // ======================== Pagination ==========================
    private static final int ROWS_PER_PAGE = 15;
    private static final int MAX_VISIBLE_PAGES = 5;

    private int currentPage = 1;
    private int totalPages = 1;
    private ObservableList<Link> currentPageData = FXCollections.observableArrayList();

    // ===============================================================
    private Crawler crawler;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            setTitleBar();
            setButtonState();
            setSummary();
            setTableView();

            crawler = new Crawler(link -> allLinks.add(link));
        });
    }

    // ============================= EVENT HANDLERS ===========================
    @FXML
    private void onStartClick() {
        String seedUrl = seedUrlField.getText().trim();

        String cleanedSeedUrl = UrlHandler.normalizeUrl(seedUrl);

        if (cleanedSeedUrl == null) {
            Application.openNotificationWindow("WARNING", "Please enter a valid seed URL before starting.");
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
        // Pastikan proses sudah selesai
        CheckingStatus status = summaryCard.getCheckingStatus();
        if (status != CheckingStatus.STOPPED && status != CheckingStatus.COMPLETED) {
            Application.openNotificationWindow("WARNING", "Export hanya bisa dilakukan setelah proses selesai.");
            return;
        }

        // Ambil hanya broken links (error ≠ kosong)
        var brokenLinks = allLinks.filtered(link -> !link.getError().isEmpty());
        if (brokenLinks.isEmpty()) {
            Application.openNotificationWindow("WARNING", "Tidak ada data broken link untuk diexport.");
            return;
        }

        // Dialog penyimpanan file
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan hasil export");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter("JSON (*.json)", "*.json")
        );

        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        // Jalankan export di background thread agar UI tidak freeze
        new Thread(() -> {
            try {
                String name = file.getName().toLowerCase();

                if (name.endsWith(".xlsx")) {
                    ExportHandler.exportToExcel(brokenLinks, file);
                } else if (name.endsWith(".csv")) {
                    ExportHandler.exportToCsv(brokenLinks, file);
                } else if (name.endsWith(".json")) {
                    ExportHandler.exportToJson(brokenLinks, file);
                } else {
                    Platform.runLater(() ->
                            Application.openNotificationWindow("WARNING", "Format file tidak dikenali."));
                    return;
                }

                Platform.runLater(() ->
                        Application.openNotificationWindow("SUCCESS", "Data berhasil diexport ke:\n" + file.getAbsolutePath()));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        Application.openNotificationWindow("ERROR", "Terjadi kesalahan saat mengekspor data."));
            }
        }).start();
    }


    // ============================= TITLE BAR ================================
    private void setTitleBar() {
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

    // ============================= BUTTON STATE =============================
    private void setButtonState() {
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
    private void setSummary() {

        // Label mengikuti nilai di Summary
        checkingStatusLabel.textProperty().bind(summaryCard.checkingStatusProperty().asString());
        totalLinksLabel.textProperty().bind(summaryCard.totalLinksProperty().asString());
        webpageLinksLabel.textProperty().bind(summaryCard.webpageLinksProperty().asString());
        brokenLinksLabel.textProperty().bind(summaryCard.brokenLinksProperty().asString());

        // Total links: langsung binding ke ukuran allLinks
        summaryCard.totalLinksProperty().bind(Bindings.size(allLinks));
        // Webpage links: hitung berapa banyak link di allLinks yang isWebpage == true
        summaryCard.webpageLinksProperty().bind(Bindings.createIntegerBinding(() -> (int) allLinks.stream().filter(Link::isWebpage).count(), allLinks));

        // Broken links: hitung link yang error-nya tidak kosong
        summaryCard.brokenLinksProperty().bind(Bindings.createIntegerBinding(() -> (int) allLinks.stream().filter(link -> !link.getError().isEmpty()).count(), allLinks));

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

    // ============================= FILTER CARD ==============================
    private void setTableFilter(FilteredList<Link> view) {
        Runnable apply = () -> view.setPredicate(link -> {
            boolean urlOk = true;
            String urlCond = urlFilterOption.getValue();
            String urlText = urlFilterField.getText();

            if (urlCond != null && !urlCond.isBlank() && urlText != null && !urlText.isBlank()) {
                String u = link.getUrl().toLowerCase();
                String q = urlText.toLowerCase();
                urlOk = switch (urlCond) {
                    case "Equals"     -> u.equals(q);
                    case "Contains"   -> u.contains(q);
                    case "Starts With"-> u.startsWith(q);
                    case "Ends With"  -> u.endsWith(q);
                    default           -> true;
                };
            }

            // ===== Status Code filter =====
            boolean statusOk = true;
            String scCond = statusCodeFilterOption.getValue();
            String scText = statusCodeFilterField.getText();

            if (scCond != null && !scCond.isBlank() && scText != null && !scText.isBlank()) {
                try {
                    int in = Integer.parseInt(scText.trim());
                    int code = link.getStatusCode();
                    statusOk = switch (scCond) {
                        case "Equals"       -> code == in;
                        case "Greater Than" -> code >  in;
                        case "Less Than"    -> code <  in;
                        default             -> true;
                    };
                } catch (NumberFormatException ignore) {
                    statusOk = true; // input tak valid → anggap filter off
                }
            }

            // hanya tampil jika lolos semua filter aktif
            return urlOk && statusOk;
        });

        // live update
        urlFilterField.textProperty().addListener((o, a, b) -> apply.run());
        urlFilterOption.valueProperty().addListener((o, a, b) -> apply.run());
        statusCodeFilterField.textProperty().addListener((o, a, b) -> apply.run());
        statusCodeFilterOption.valueProperty().addListener((o, a, b) -> apply.run());
    }

    // ============================= RESULT TABLE =============================
    private void setTableView() {

        // Biar kolom terakhir selalu memenuhi ukuan tabel sisa
        brokenLinkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Sumber data
        errorColumn.setCellValueFactory(cell -> cell.getValue().errorProperty());
        urlColumn.setCellValueFactory(cell -> cell.getValue().urlProperty());

        // Filter layer 1: hanya broken links
        FilteredList<Link> brokenOnly = new FilteredList<>(allLinks, link -> !link.getError().isEmpty());

        // Filter layer 2: filter view
        FilteredList<Link> view = new FilteredList<>(brokenOnly, l -> true);

        // Set ke tabel
        brokenLinkTable.setItems(view);

        // Hook filter UI -> predicate 'view'
        setTableFilter(view);

        // Kalau baris di klik maka akan buka jendela baru
        brokenLinkTable.setRowFactory(tv -> {
            TableRow<Link> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    Link clickedLink = row.getItem();
                    Application.openLinkWindow(clickedLink);
                }
            });
            return row;
        });

        errorColumn.setCellFactory(col -> new TableCell<>() {
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

                    // warna merah untuk error dari status code
                    if (code >= 400 && code < 600) {
                        setStyle("-fx-text-fill: #ef4444;");
                    } else {
                        setStyle("-fx-text-fill: #f9fafb;");
                    }
                }
            }
        });

        // hyperlink di kolom URL
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
                    setGraphic(link);
                }
            }
        });

        setPagination(view);
    }

    // ============================= PAGINATION ===============================
    private void setPagination(FilteredList<Link> view) {
        updatePagination(view);

        // kalau data berubah (misal filter diganti), pagination reset
        view.addListener((javafx.collections.ListChangeListener<Link>) c -> {
            currentPage = 1;
            updatePagination(view);
        });
    }

    private void updatePagination(FilteredList<Link> view) {
        int totalRows = view.size();
        totalPages = (int) Math.ceil((double) totalRows / ROWS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        // pastikan current page masih valid
        if (currentPage > totalPages) currentPage = totalPages;

        // ambil subset sesuai halaman
        int fromIndex = (currentPage - 1) * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, totalRows);
        currentPageData.setAll(view.subList(fromIndex, toIndex));

        brokenLinkTable.setItems(currentPageData);
        renderPaginationButtons(view);
    }

    private void renderPaginationButtons(FilteredList<Link> view) {
        paginationBar.getChildren().clear();

        // Tombol PREV
        Button prevBtn = new Button("<<");
        prevBtn.getStyleClass().addAll("pagination-btn", "prev");
        prevBtn.setDisable(currentPage <= 1);
        prevBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                updatePagination(view);
            }
        });
        paginationBar.getChildren().add(prevBtn);

        // Tombol nomor halaman
        int startPage = Math.max(1, currentPage - MAX_VISIBLE_PAGES / 2);
        int endPage = Math.min(startPage + MAX_VISIBLE_PAGES - 1, totalPages);
        if (endPage - startPage + 1 < MAX_VISIBLE_PAGES)
            startPage = Math.max(1, endPage - MAX_VISIBLE_PAGES + 1);

        for (int i = startPage; i <= endPage; i++) {
            Button pageBtn = new Button(String.valueOf(i));
            pageBtn.getStyleClass().add("pagination-btn");
            if (i == currentPage) {
                pageBtn.getStyleClass().add("active");
            }

            final int pageIndex = i;
            pageBtn.setOnAction(e -> {
                currentPage = pageIndex;
                updatePagination(view);
            });

            paginationBar.getChildren().add(pageBtn);
        }

        // Tombol NEXT
        Button nextBtn = new Button(">>");
        nextBtn.getStyleClass().addAll("pagination-btn", "next");
        nextBtn.setDisable(currentPage >= totalPages);
        nextBtn.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                updatePagination(view);
            }
        });
        paginationBar.getChildren().add(nextBtn);
    }
}
