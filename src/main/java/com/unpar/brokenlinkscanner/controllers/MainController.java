package com.unpar.brokenlinkscanner.controllers;

import com.unpar.brokenlinkscanner.Application;
import com.unpar.brokenlinkscanner.services.Crawler;
import com.unpar.brokenlinkscanner.services.Exporter;
import com.unpar.brokenlinkscanner.models.Status;
import com.unpar.brokenlinkscanner.models.Link;
import com.unpar.brokenlinkscanner.models.Summary;
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
public class MainController {
    // ======================== GUI Component ========================
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

    // ======================== Data Model ===========================
    // Untuk menyimpan seluruh tautan hasil crawling
    private final ObservableList<Link> allLinks = FXCollections.observableArrayList();
    // Untuk menyimpan tautan halaman
    private final FilteredList<Link> webpageLinks = new FilteredList<>(allLinks, link -> link.isWebpage() == true);
    // Untuk menyimpan tautan rusak
    private final FilteredList<Link> brokenLinks = new FilteredList<>(allLinks, link -> !link.getError().isEmpty());
    // Untuk menyimpan data summary dari crawling
    private final Summary summary = new Summary();

    // ======================== Drag Window ==========================
    // Untuk menyimpan posisi cursor pada sumbu X
    private double xOffset;
    // Untuk menyimpan posisi cursor pada sumbu Y
    private double yOffset;

    // ======================== Pagination ==========================
    // Untuk menyimpan daftar link perhalaman pada pagination
    private final ObservableList<Link> paginationData = FXCollections.observableArrayList();
    // Jumlah baris tabel perhalaman pagination
    private static final int ROWS_PER_PAGE = 15;
    // Jumlah halaman yang terlihat pada pagination
    private static final int MAX_VISIBLE_PAGES = 5;
    // Halaman saat ini pada pagination
    private int currentPage = 1;
    // Total jumlah halaman pagination
    private int totalPages = 1;

    // ===============================================================
    // Untuk menyimpan instance dari kelas Crawler
    private Crawler crawler;



    @FXML
    public void initialize() {
        setupUncaughtExceptionHandler();

        Platform.runLater(() -> {
            setTitleBar();
            setButtonState();
            setSummaryCard();
            setTableView();
            setFilterCard();
            setPagination();

            /*
             * Buat instance kelas Crawler dan kirim sebuah fungsi (lambda expresion) untuk
             * memasukan objek link ke list allLinks. Lambda ini nanti bakal jadi function
             * interface Consumer dengan tipe data paramsnya adalah objek Link.
             */
            crawler = new Crawler(link -> allLinks.add(link));
        });
    }

    // ============================= EVENT HANDLERS ===========================

    /**
     * Event handler untuk tombol "Start".
     *
     * Fungsi ini digunakan untuk memulai proses crawling.
     * Semua proses crawling dijalankan di virtual thread supaya UI tetap responsif.
     */
    @FXML
    private void onStartClick() {
        try {
            // Ambil input dari field dan hapus spasi di awal/akhir
            String seedUrl = seedUrlField.getText().trim();

            // Normalisasi URL supaya formatnya konsisten
            String cleanedSeedUrl = UrlHandler.normalizeUrl(seedUrl, true);

            // Kalau seed URL kosong atau tidak valid → tampilkan pesan
            if (cleanedSeedUrl == null) {
                showNofication("WARNING", "Please enter a valid seed URL before starting.");
                return;
            }

            // Update URL di GUI dengan yang sudah di normalisasi
            seedUrlField.setText(cleanedSeedUrl);

            // Bersihkan semua data link lama di tabel dan struktur data internal
            allLinks.clear();

            // Ubah status jadi CHECKING
            summary.setStatus(Status.CHECKING);

            /*
             * Jalankan proses crawling di thread yang berbeda dengan thread yang
             * menjalankan GUI. Pake virtual thread biar lebih ringan karna thread biasa 1:1
             * dengan thread laptop.
             */
            Thread.startVirtualThread(() -> {
                try {
                    // Simpan waktu mulai proses
                    summary.setStartTime(System.currentTimeMillis());

                    // Mulai proses crawling
                    crawler.start(cleanedSeedUrl);

                    // Simpan waktu selesai proses
                    summary.setEndTime(System.currentTimeMillis());


                    /*
                     * Kalau crawling selesai secara normal (bukan dihentikan user),
                     * maka ubah status menjadi COMPLETED.
                     * Harus lewat Platform.runLater karena menyentuh UI.
                     */
                    if (!crawler.isStoppedByUser()) {
                        Platform.runLater(() -> summary.setStatus(Status.COMPLETED));
                    }

                } catch (Exception e) {
                    showNofication("ERROR", e.getMessage());
                }
            });
        } catch (Exception e) {
            showNofication("ERROR", e.getMessage());
        }
    }

    /**
     * Event handler untuk tombol "Stop".
     *
     * Digunakan untuk menghentikan proses crawling yang sedang berjalan.
     */
    @FXML
    private void onStopClick() {
        try {
            // Pastikan crawler sudah pernah dibuat (tidak null)
            if (crawler != null) {
                // Hentikan proses crawling
                crawler.stop();

                // Update status summary menjadi STOPPED
                summary.setStatus(Status.STOPPED);
            }
        } catch (Exception e) {
            showNofication("ERROR", e.getMessage());
        }

    }

    /**
     * Event handler untuk tombol "Export"
     *
     * Digunakan untuk menyimpan data broken link ke file lokal dalam format Excel
     * (.xlsx)
     */
    @FXML
    private void onExportClick() {
        try {
            // Dapatkan status proses pengecekan saat ini
            Status status = summary.getStatus();

            // Export hanya bisa dilakukan setelah proses selesai
            if (status != Status.STOPPED && status != Status.COMPLETED) {
                showNofication("WARNING", "Export is only available after the process is finished.");
                return;
            }

            // Tidak ada broken link → tidak perlu ekspor
            if (brokenLinks.isEmpty()) {
                showNofication("WARNING", "There are no broken links to export.");
                return;
            }

            // ============ FILE CHOOSER ============
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Excel File");

            // Hanya izinkan .xlsx
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));

            File file = chooser.showSaveDialog(null);
            if (file == null) {
                return; // User cancel
            }

            // Pastikan ekstensi selalu .xlsx meskipun user lupa menulis
            if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                file = new File(file.getAbsolutePath() + ".xlsx");
            }

            // ============ EKSEKUSI EXPORT ============
            File finalFile = file;

            Thread.startVirtualThread(() -> {
                try {
                    Exporter exporter = new Exporter(summary, brokenLinks);
                    exporter.save(finalFile);

                    showNofication("SUCCESS",
                            "Data has been successfully exported to:\n" + finalFile.getAbsolutePath());
                } catch (Exception e) {
                    showNofication("ERROR", e.getMessage());
                }
            });
        } catch (Exception e) {
            showNofication("ERROR", e.getMessage());
        }
    }

    // ============================= TITLE BAR ================================
    private void setTitleBar() {
        // Ambil reference ke stage (window) dari titleBar
        Stage stage = (Stage) titleBar.getScene().getWindow();

        /*
         * Waktu user neken mouse di area title bar,
         * kita simpan posisi awal kursornya relatif terhadap window.
         * Data ini nanti dipakai buat ngatur seberapa jauh window-nya harus digeser.
         */
        titleBar.setOnMousePressed((MouseEvent e) -> {
            xOffset = e.getSceneX(); // jarak X dari pojok kiri window
            yOffset = e.getSceneY(); // jarak Y dari pojok atas window
        });

        /*
         * Kalau user lagi drag (tekan sambil gerak),
         * kita ubah posisi window berdasarkan posisi kursor di layar.
         */
        titleBar.setOnMouseDragged((MouseEvent e) -> {
            stage.setX(e.getScreenX() - xOffset); // geser window secara horizontal
            stage.setY(e.getScreenY() - yOffset); // geser window secara vertikal
        });

        // Perkecil window saat tombol minimize diklik
        minimizeBtn.setOnAction(e -> stage.setIconified(true));
        // Perbesar window saat tombol maximaize diklik
        maximizeBtn.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));
        // Tutup window saat tombol close diklik
        closeBtn.setOnAction(e -> stage.close());
    }

    // ============================= BUTTON STATE =============================

    /**
     * Method untuk mengatur perilaku tombol (Start, Stop, Export) berdasarkan
     * status aplikasi.
     */
    private void setButtonState() {
        /*
         * Tambahkan listener ke properti status di objek Summary.
         * Listener ini akan terpanggil setiap kali nilai status berubah.
         *
         * Parameter:
         * - obs : objek Observable
         * - old : status lama sebelum berubah
         * - status : status baru setelah perubahan
         */
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

    // ============================= RESULT TABLE =============================

    /**
     * Method untuk mengatur tampilan dan perilaku tabel yang menampilkan daftar
     * broken link.
     */
    private void setTableView() {

        // Biar kolom terakhir selalu memenuhi ukuan tabel sisa
        brokenLinkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Set ke tabel
        brokenLinkTable.setItems(brokenLinks);

        // Sumber data tiap kolom
        errorColumn.setCellValueFactory(cell -> cell.getValue().errorProperty());
        urlColumn.setCellValueFactory(cell -> cell.getValue().urlProperty());

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

        // Warna pada kolom error
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
                        setStyle("-fx-text-fill: -grey-dark; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: -red; -fx-font-weight: bold;");
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
                        showNofication("ERROR", ex.getMessage());
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

    // ============================= SUMMARY CARD =============================
    private void setSummaryCard() {
        // Label mengikuti nilai di Summary
        statusLabel.textProperty().bind(summary.statusProperty().asString());
        allLinksCountLabel.textProperty().bind(summary.allLinksCountProperty().asString());
        webpageLinksCountLabel.textProperty().bind(summary.webpageLinksCountProperty().asString());
        brokenLinksCountLabel.textProperty().bind(summary.brokenLinksCountProperty().asString());

        summary.allLinksCountProperty().bind(Bindings.size(allLinks));
        summary.webpageLinksCountProperty().bind(Bindings.size(webpageLinks));
        summary.brokenLinksCountProperty().bind(Bindings.createIntegerBinding(
                () -> (int) allLinks.stream().filter(l -> !l.getError().isEmpty()).count(), allLinks));

        // Warna dinamis berdasarkan status
        summary.statusProperty().addListener((obs, old, status) -> {
            switch (status) {
                case IDLE -> statusLabel.setStyle("-fx-text-fill: #f9fafb;"); // putih
                case CHECKING -> statusLabel.setStyle("-fx-text-fill: #60a5fa;"); // biru
                case STOPPED -> statusLabel.setStyle("-fx-text-fill: #ef4444;"); // merah
                case COMPLETED -> statusLabel.setStyle("-fx-text-fill: #10b981;"); // hijau
            }
        });

        summary.setStatus(Status.STOPPED);
        summary.setStatus(Status.IDLE);
    }

    // ============================= FILTER CARD ==============================

    /**
     * Method untuk mengatur logika filter pada tabel
     */
    private void setFilterCard() {
        // Runnable dipakai agar kita bisa memanggil filter.run() berkali-kali
        Runnable filter = () -> brokenLinks.setPredicate(link -> {

            /*
             * Jika link tidak memiliki error, otomatis tidak ditampilkan.
             * Karena hanya broken links (yang punya error) yang relevan untuk difilter.
             */
            if (link.getError().isEmpty()) {
                return false;
            }

            // ================== URL filter ==================
            // Penanda untuk evaluasi filter URL
            boolean urlOk = true;
            // Ambil kondisi filter
            String urlCond = urlFilterOption.getValue();
            // Ambil teks pencarian yang dimasukkan user
            String urlText = urlFilterField.getText();

            /*
             * Filter URL hanya dijalankan jika kondisi (option) dan teks input tidak
             * kosong. Kalau user belum mengisi, filter dianggap nonaktif (urlOk = true).
             */
            if (urlCond != null && !urlCond.isBlank() && urlText != null && !urlText.isBlank()) {
                // URL dari link
                String u = link.getUrl().toLowerCase();
                // Teks pencarian
                String q = urlText.toLowerCase();

                urlOk = switch (urlCond) {
                    case "Equals" -> u.equals(q); // URL harus sama persis
                    case "Contains" -> u.contains(q); // URL mengandung teks pencarian
                    case "Starts With" -> u.startsWith(q); // URL diawali teks pencarian
                    case "Ends With" -> u.endsWith(q); // URL diakhiri teks pencarian
                    default -> true;
                };
            }

            // ================== Status Code filter ==================
            // Penanda untuk evaluasi filter Status Code
            boolean scOk = true;
            // Ambil kondisi filter status code
            String scCond = statusCodeFilterOption.getValue();
            // Ambil nilai status code yang dimasukkan user
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
                    scOk = true; // input tak valid
                }
            }

            // hanya tampil jika lolos semua filter aktif
            return urlOk && scOk;
        });

        /*
         * Tiap kali pengguna mengubah teks atau option di field filter,
         * Runnable filter.run() akan dipanggil, sehingga hasil filter diperbarui
         * langsung.
         */
        urlFilterField.textProperty().addListener((o, a, b) -> filter.run());
        urlFilterOption.valueProperty().addListener((o, a, b) -> filter.run());
        statusCodeFilterField.textProperty().addListener((o, a, b) -> filter.run());
        statusCodeFilterOption.valueProperty().addListener((o, a, b) -> filter.run());
    }

    // ============================= PAGINATION ===============================
    private void setPagination() {
        // Pertama kali, langsung render pagination berdasarkan data awal
        updatePagination();

        // Jika brokenLinks berubah (data nambah, berkurang, filter aktif) maka update pagination
        brokenLinks.addListener((javafx.collections.ListChangeListener<Link>) c -> updatePagination());
    }

    private void updatePagination() {
        // jumlah semua data/baris
        int totalRows = brokenLinks.size();

        // hitung jumlah halaman
        totalPages = (int) Math.ceil((double) totalRows / ROWS_PER_PAGE);

        // Jika tidak ada data, tetap minimal 1 halaman
        if (totalPages == 0) {
            totalPages = 1;
        }

        // Pastikan currentPage tidak melebihi total halaman
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        // baris pertama pada halaman saat ini
        int fromIndex = (currentPage - 1) * ROWS_PER_PAGE;
        // baris terakhir pada halaman saat ini (tidak di sertakan)
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, totalRows);

        if (totalRows > 0) {
            // Ambil subset data dari brokenLinks untuk ditampilkan di halaman saat ini
            paginationData.setAll(brokenLinks.subList(fromIndex, toIndex));
        } else {
            paginationData.clear();
        }

        // Pasang subset data ini ke tabel
        brokenLinkTable.setItems(paginationData);

        // Render ulang tombol navigasi (Prev, angka halaman, Next)
        renderPaginationButtons();

        // ================ TABLE INFORMATION ================
        int itemStart = (totalRows == 0) ? 0 : fromIndex + 1;
        int itemEnd = (totalRows == 0) ? 0 : toIndex;

        // page X of Y
        pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);
        // items A–B of C
        itemInfoLabel.setText("Item " + itemStart + "-" + itemEnd + " of " + totalRows);
    }

    private void renderPaginationButtons() {
        // Bersihkan semua tombol lama
        paginationBar.getChildren().clear();

        // Tombol PREV
        Button prevBtn = new Button("<<");
        // tambahkan CSS class
        prevBtn.getStyleClass().addAll("pagination-btn", "prev");
        // nonaktif kalau di halaman pertama
        prevBtn.setDisable(currentPage <= 1);

        // Aksi saat tombol diklik, pindah ke halaman sebelumnya
        prevBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                updatePagination();
            }
        });
        // Tambahkan ke bar navigasi
        paginationBar.getChildren().add(prevBtn);

        /*
         * Tentukan rentang nomor halaman yang mau ditampilkan.
         * Misal: kalau MAX_VISIBLE_PAGES = 5 dan currentPage = 7,
         * maka bisa muncul halaman 5-9 agar posisi aktif tetap di tengah.
         */
        int startPage = Math.max(1, currentPage - MAX_VISIBLE_PAGES / 2);
        int endPage = Math.min(startPage + MAX_VISIBLE_PAGES - 1, totalPages);

        // Kalau jumlah halaman kurang dari batas maksimum, geser startPage supaya pas
        if (endPage - startPage + 1 < MAX_VISIBLE_PAGES) {
            startPage = Math.max(1, endPage - MAX_VISIBLE_PAGES + 1);
        }

        /*
         * Buat tombol per halaman
         * Tiap tombol menampilkan nomor halaman dan bisa diklik untuk berpindah.
         */
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

        // Tombol NEXT
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

    // ============================= NOTIFICATION HANDLER ===============================

    private void showNofication(String type, String message) {
        String msg = (message == null || message.isBlank())
                ? "Unknown error."
                : message;

        Platform.runLater(() -> Application.openNotificationWindow(type, msg));
    }

    /**
     * Method untuk menangani semua error yang tidak tertangkap (uncaught
     * exceptions)
     * di seluruh aplikasi, termasuk error yang terjadi di event handler JavaFX,
     * virtual thread, atau background task lainnya.
     *
     * Dengan memasang DefaultUncaughtExceptionHandler, setiap error yang tidak
     * ditangkap dengan try–catch akan otomatis diarahkan ke GUI melalui
     * showNofication(), sehingga error dari kelas manapun bisa tampil di layar.
     */
    private void setupUncaughtExceptionHandler() {
        // Pasang handler global untuk semua exception yang tidak tertangkap
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {

            // Ambil pesan error asli dari exception
            String message = throwable.getMessage();

            // Jika pesan kosong atau null, pakai toString() biar tetap ada informasi
            // error-nya
            if (message == null || message.isBlank()) {
                message = throwable.toString();
            }

            // Panggil notifikasi GUI untuk menampilkan error ke layar
            showNofication("ERROR", message);
        });
    }

}
