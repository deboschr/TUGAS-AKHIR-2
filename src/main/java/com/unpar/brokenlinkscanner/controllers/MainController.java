package com.unpar.brokenlinkscanner.controllers;

import com.unpar.brokenlinkscanner.Application;
import com.unpar.brokenlinkscanner.services.Crawler;
import com.unpar.brokenlinkscanner.services.Exporter;
import com.unpar.brokenlinkscanner.models.Status;
import com.unpar.brokenlinkscanner.models.Link;
import com.unpar.brokenlinkscanner.models.Summary;
import com.unpar.brokenlinkscanner.utils.LinkReceiver;
import com.unpar.brokenlinkscanner.utils.UrlHandler;

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

/**
 * Controller utama aplikasi Broken Link Checker.
 */
public class MainController implements LinkReceiver {

    // =================================================================================================
    @FXML
    private HBox titleBar, paginationBar;
    @FXML
    private Button minimizeBtn, maximizeBtn, closeBtn, startBtn, stopBtn, exportBtn;
    @FXML
    private Label statusLabel, allLinksCountLabel, webpageLinksCountLabel, brokenLinksCountLabel;
    @FXML
    private Label pageInfoLabel, itemInfoLabel;
    @FXML
    private TextField seedUrlField, urlFilterField, statusCodeFilterField;
    @FXML
    private ComboBox<String> urlFilterOption, statusCodeFilterOption;
    @FXML
    private TableView<Link> brokenLinkTable;
    @FXML
    private TableColumn<Link, String> errorColumn, urlColumn;

    // =================================================================================================

    private final ObservableList<Link> allLinks = FXCollections.observableArrayList();

    private final FilteredList<Link> webpageLinks = new FilteredList<>(allLinks, link -> link.isWebpage() == true);

    private final FilteredList<Link> brokenLinks = new FilteredList<>(allLinks, link -> !link.getError().isEmpty());

    private final ObservableList<Link> paginationData = FXCollections.observableArrayList();

    private static final int ROWS_PER_PAGE = 15;

    private static final int MAX_VISIBLE_PAGES = 5;

    private int currentPage = 1;

    private int totalPages = 1;

    private double xOffset;

    private double yOffset;

    private Crawler crawler;

    private final Summary summary = new Summary();

    @FXML
    public void initialize() {
        setupUncaughtExceptionHandler();

        Platform.runLater(() -> {
            setTitleBar();
            setButtonState();
            setSummaryCard();
            setFilterCard();
            setTableView();
            setPagination();


            // IF UNPAR (OKE)
            seedUrlField.setText("https://informatika.unpar.ac.id");

            // IF UNPAS (OKE)
            // seedUrlField.setText("https://if.unpas.ac.id");

            // IF UNPAD (OKE)
            // seedUrlField.setText("https://informatika.unpad.ac.id");

            // IF UNIKOM (OKE) (RADA KECIL)
            // seedUrlField.setText("https://if.unikom.ac.id");


            // ==================================================================

            // MAT ITB (BLOCK)
            // seedUrlField.setText("https://fmipa.itb.ac.id");

            // IF TELKOM (BLOCK)
            // seedUrlField.setText("https://soc.telkomuniversity.ac.id");


            // IF BINUS => SCHOOL OF COMPUTER SCIENCE BINUS (TERLALU BESAR)
            // seedUrlField.setText("https://socs.binus.ac.id");

            // IF ITB => Sekolah Teknik Elektro dan Informatika (Terlalu Besar)
            // seedUrlField.setText("https://stei.itb.ac.id");


            // IF MARNAT (LAMBAT)
            // seedUrlField.setText("https://it.maranatha.edu");

            // IF UNJANI
            // seedUrlField.setText("https://if.unjani.ac.id");

            // MAT UNPAR (TERLALU KECIL)
            // seedUrlField.setText("https://matematika.unpar.ac.id");


            crawler = new Crawler(this);
        });
    }

    @Override
    public void receive(Link link) {
        Platform.runLater(() -> allLinks.add(link));
    }

    // =============== EVENT HANDLER ===============
    @FXML
    private void onStartClick() {
        try {

            String seedUrl = seedUrlField.getText().trim();


            String cleanedSeedUrl = UrlHandler.normalizeUrl(seedUrl, false);


            if (cleanedSeedUrl == null) {
                showNotification("WARNING", "Please enter a valid seed URL before starting.");
                return;
            }


            seedUrlField.setText(cleanedSeedUrl);


            allLinks.clear();


            summary.setStatus(Status.CHECKING);

            Thread.startVirtualThread(() -> {
                try {

                    summary.setStartTime(System.currentTimeMillis());

                    crawler.start(cleanedSeedUrl);

                    summary.setEndTime(System.currentTimeMillis());

                    if (!crawler.isStoppedByUser()) {
                        Platform.runLater(() -> summary.setStatus(Status.COMPLETED));
                    }

                } catch (Exception e) {
                    showNotification("ERROR", e.getMessage());
                }
            });
        } catch (Exception e) {
            showNotification("ERROR", e.getMessage());
        }
    }

    @FXML
    private void onStopClick() {
        try {

            if (crawler != null) {

                crawler.stop();


                summary.setStatus(Status.STOPPED);
            }
        } catch (Exception e) {
            showNotification("ERROR", e.getMessage());
        }

    }

    @FXML
    private void onExportClick() {
        try {

            Status status = summary.getStatus();


            if (status != Status.STOPPED && status != Status.COMPLETED) {
                showNotification("WARNING", "Export is only available after the process is finished.");
                return;
            }


            if (brokenLinks.isEmpty()) {
                showNotification("WARNING", "There are no broken links to export.");
                return;
            }


            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Excel File");


            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));

            File file = chooser.showSaveDialog(null);
            if (file == null) {
                return;
            }


            if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                file = new File(file.getAbsolutePath() + ".xlsx");
            }


            File finalFile = file;

            Thread.startVirtualThread(() -> {
                try {
                    Exporter exporter = new Exporter(summary, brokenLinks);
                    exporter.save(finalFile);

                    showNotification("SUCCESS", "Data has been successfully exported to:\n" + finalFile.getAbsolutePath());
                } catch (Exception e) {
                    showNotification("ERROR", e.getMessage());
                }
            });
        } catch (Exception e) {
            showNotification("ERROR", e.getMessage());
        }
    }

    // =============== SET UP GUI ===============
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

    private void setButtonState() {

        summary.statusProperty().addListener((obs, old, status) -> {
            switch (status) {
                case IDLE -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    exportBtn.setDisable(true);
                }
                case CHECKING -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(false);
                    exportBtn.setDisable(true);
                }
                case STOPPED, COMPLETED -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    exportBtn.setDisable(false);
                }
            }
        });
    }

    private void setTableView() {

        brokenLinkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        brokenLinkTable.setItems(brokenLinks);

        errorColumn.setCellValueFactory(cell -> cell.getValue().errorProperty());
        urlColumn.setCellValueFactory(cell -> cell.getValue().urlProperty());

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


                    if (code >= 400 && code < 600) {
                        setStyle("-fx-text-fill: -grey-dark; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: -red; -fx-font-weight: bold;");
                    }
                }
            }
        });


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
                        showNotification("ERROR", ex.getMessage());
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

    }

    private void setSummaryCard() {

        statusLabel.textProperty().bind(summary.statusProperty().asString());
        allLinksCountLabel.textProperty().bind(summary.allLinksCountProperty().asString());
        webpageLinksCountLabel.textProperty().bind(summary.webpageLinksCountProperty().asString());
        brokenLinksCountLabel.textProperty().bind(summary.brokenLinksCountProperty().asString());

        summary.allLinksCountProperty().bind(Bindings.size(allLinks));
        summary.webpageLinksCountProperty().bind(Bindings.size(webpageLinks));
        summary.brokenLinksCountProperty().bind(Bindings.createIntegerBinding(() -> (int) allLinks.stream().filter(l -> !l.getError().isEmpty()).count(), allLinks));

        summary.statusProperty().addListener((obs, old, status) -> {
            switch (status) {
                case IDLE -> statusLabel.setStyle("-fx-text-fill: #f9fafb;");
                case CHECKING -> statusLabel.setStyle("-fx-text-fill: #60a5fa;");
                case STOPPED -> statusLabel.setStyle("-fx-text-fill: #ef4444;");
                case COMPLETED -> statusLabel.setStyle("-fx-text-fill: #10b981;");
            }
        });

        summary.setStatus(Status.STOPPED);
        summary.setStatus(Status.IDLE);
    }

    private void setFilterCard() {

        Runnable filter = () -> brokenLinks.setPredicate(link -> {

            if (link.getError().isEmpty()) {
                return false;
            }

            boolean urlOk = true;

            String urlCond = urlFilterOption.getValue();

            String urlText = urlFilterField.getText();

            if (urlCond != null && !urlCond.isBlank() && urlText != null && !urlText.isBlank()) {

                String u = link.getUrl().toLowerCase();

                String q = urlText.toLowerCase();

                urlOk = switch (urlCond) {
                    case "Equals" -> u.equals(q);
                    case "Contains" -> u.contains(q);
                    case "Starts With" -> u.startsWith(q);
                    case "Ends With" -> u.endsWith(q);
                    default -> true;
                };
            }


            boolean scOk = true;

            String scCond = statusCodeFilterOption.getValue();

            String scText = statusCodeFilterField.getText();

            if (scCond != null && !scCond.isBlank() && scText != null && !scText.isBlank()) {
                try {
                    int in = Integer.parseInt(scText.trim());
                    int code = link.getStatusCode();
                    scOk = switch (scCond) {
                        case "Equals" -> code == in;
                        case "Greater Than" -> code > in;
                        case "Less Than" -> code < in;
                        default -> true;
                    };
                } catch (NumberFormatException ignore) {
                    scOk = true;
                }
            }


            return urlOk && scOk;
        });

        urlFilterField.textProperty().addListener((o, a, b) -> filter.run());
        urlFilterOption.valueProperty().addListener((o, a, b) -> filter.run());
        statusCodeFilterField.textProperty().addListener((o, a, b) -> filter.run());
        statusCodeFilterOption.valueProperty().addListener((o, a, b) -> filter.run());
    }

    private void setPagination() {
        updatePagination();
        brokenLinks.addListener((javafx.collections.ListChangeListener<Link>) c -> updatePagination());
    }

    private void updatePagination() {

        int totalRows = brokenLinks.size();
        totalPages = (int) Math.ceil((double) totalRows / ROWS_PER_PAGE);


        if (totalPages == 0) {
            totalPages = 1;
        }

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        int fromIndex = (currentPage - 1) * ROWS_PER_PAGE;

        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, totalRows);

        if (totalRows > 0) {
            paginationData.setAll(brokenLinks.subList(fromIndex, toIndex));
        } else {
            paginationData.clear();
        }

        brokenLinkTable.setItems(paginationData);

        renderPaginationButtons();

        int itemStart = (totalRows == 0) ? 0 : fromIndex + 1;
        int itemEnd = (totalRows == 0) ? 0 : toIndex;

        pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);
        itemInfoLabel.setText("Item " + itemStart + "-" + itemEnd + " of " + totalRows);
    }

    private void renderPaginationButtons() {

        paginationBar.getChildren().clear();

        Button prevBtn = new Button("<<");

        prevBtn.getStyleClass().addAll("pagination-btn", "prev");
        prevBtn.setDisable(currentPage <= 1);
        prevBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                updatePagination();
            }
        });

        paginationBar.getChildren().add(prevBtn);

        int startPage = Math.max(1, currentPage - MAX_VISIBLE_PAGES / 2);
        int endPage = Math.min(startPage + MAX_VISIBLE_PAGES - 1, totalPages);


        if (endPage - startPage + 1 < MAX_VISIBLE_PAGES) {
            startPage = Math.max(1, endPage - MAX_VISIBLE_PAGES + 1);
        }

        for (int i = startPage; i <= endPage; i++) {
            Button pageBtn = new Button(String.valueOf(i));
            pageBtn.getStyleClass().add("pagination-btn");

            if (i == currentPage) {
                pageBtn.getStyleClass().add("active");
            }

            final int pageIndex = i;
            pageBtn.setOnAction(e -> {
                currentPage = pageIndex;
                updatePagination();
            });

            paginationBar.getChildren().add(pageBtn);
        }


        Button nextBtn = new Button(">>");
        nextBtn.getStyleClass().addAll("pagination-btn", "next");
        nextBtn.setDisable(currentPage >= totalPages);
        nextBtn.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                updatePagination();
            }
        });
        paginationBar.getChildren().add(nextBtn);
    }

    // =============== UTILS ===============
    private void showNotification(String type, String msg) {
        String message = (msg == null || msg.isBlank()) ? "Unknown error." : msg;

        Platform.runLater(() -> Application.openNotificationWindow(type, message));
    }

    private void setupUncaughtExceptionHandler() {

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {

            String message = throwable.getMessage();

            if (message == null || message.isBlank()) {
                message = throwable.toString();
            }

            showNotification("ERROR", message);
        });
    }

}
