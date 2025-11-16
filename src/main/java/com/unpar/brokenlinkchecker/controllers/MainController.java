package com.unpar.brokenlinkchecker.controllers;

import com.unpar.brokenlinkchecker.Application;
import com.unpar.brokenlinkchecker.cores.Crawler;
import com.unpar.brokenlinkchecker.models.Status;
import com.unpar.brokenlinkchecker.models.Link;
import com.unpar.brokenlinkchecker.models.Summary;
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
import javafx.stage.Stage;

import java.awt.*;
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
    private TextField seedUrlField, urlFilterField, statusCodeFilterField;
    @FXML
    private ComboBox<String> urlFilterOption, statusCodeFilterOption;
    @FXML
    private TableView<Link> brokenLinkTable;
    @FXML
    private TableColumn<Link, String> errorColumn, urlColumn;

    // ======================== Data Model ===========================
    private final ObservableList<Link> allLinks = FXCollections.observableArrayList();
    private final FilteredList<Link> webpageLinks = new FilteredList<>(allLinks, link -> link.isWebpage() == true);
    private final FilteredList<Link> brokenLinks = new FilteredList<>(allLinks, link -> !link.getError().isEmpty());
    private final Summary summary = new Summary();

    // ======================== Drag Window ==========================
    private double xOffset;
    private double yOffset;

    // ======================== Pagination ==========================
    private static final int ROWS_PER_PAGE = 15;
    private static final int MAX_VISIBLE_PAGES = 5;

    private int currentPage = 1;
    private int totalPages = 1;

    private final ObservableList<Link> currentPageData = FXCollections.observableArrayList();

    private final java.util.Queue<Link> pendingLinks = new java.util.concurrent.ConcurrentLinkedQueue<>();
    // ===============================================================
    private Crawler crawler;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            setTitleBar();
            setButtonState();
            serSummaryCard();
            setTableView();
            setFilterCard();
            setPagination();

            // Bikin instance Crawler dengan mengirim comsumer pendingLinks
            crawler = new Crawler(link -> pendingLinks.offer(link));

            // Timer buat memindahkan data dari pendingLinks ke allLinks di JavaFX thread
            javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
                @Override
                public void handle(long now) {
                    int batch = 0;
                    Link link;

                    // Batasi jumlah per frame biar UI nggak nge-lag
                    while (batch < 50 && (link = pendingLinks.poll()) != null) {
                        allLinks.add(link);
                        batch++;
                    }
                }
            };
            timer.start();
        });
    }

    // ============================= EVENT HANDLERS ===========================

    /**
     * Event handler untuk tombol "Start".
     *
     * Method ini dijalankan saat pengguna menekan tombol "Start".
     * Prosesnya:
     * - Mengambil URL awal dari input field dan membersihkan formatnya
     * - Memvalidasi URL yang dimasukkan
     * - Mengosongkan data hasil crawling sebelumnya
     * - Menetapkan status aplikasi menjadi CHECKING
     * - Menjalankan proses crawling di background thread
     *
     * Proses crawling dilakukan di thread terpisah agar UI tetap responsif
     * dan tidak freeze selama pemeriksaan tautan berlangsung.
     */
    @FXML
    private void onStartClick() {
        // Ambil teks dari field input seed URL dan hilangkan spasi di awal/akhir
        String seedUrl = seedUrlField.getText().trim();

        // Normalisasi URL
        String cleanedSeedUrl = UrlHandler.normalizeUrl(seedUrl);

        // Jika hasil normalisasi null berarti URL tidak valid
        if (cleanedSeedUrl == null) {
            // Tampilkan pesan peringatan ke pengguna
            Application.openNotificationWindow("WARNING", "Please enter a valid seed URL before starting.");
            return; // Hentikan proses start
        }

        // Kosongkan semua data link lama di tabel dan list internal
        allLinks.clear();

        // Ubah status summary jadi CHECKING untuk menandakan proses sedang berjalan
        summary.setStatus(Status.CHECKING);

        // Jalankan proses crawling di background thread.
        new Thread(() -> {
            // Mulai proses crawling dengan seed URL yang sudah dibersihkan
            crawler.start(cleanedSeedUrl);

            /*
             * Jika proses crawling selesai secara normal (bukan dihentikan manual),
             * ubah status aplikasi menjadi COMPLETED.
             * Platform.runLater() dipakai agar update status dijalankan
             * di JavaFX Application Thread (karena summary di-bind ke GUI).
             */
            if (!crawler.isStopped()) {
                Platform.runLater(() -> summary.setStatus(Status.COMPLETED));
            }
        }).start();
    }

    /**
     * Event handler untuk tombol "Stop".
     *
     * Digunakan untuk menghentikan proses crawling yang sedang berjalan.
     */
    @FXML
    private void onStopClick() {
        // Pastikan crawler sudah pernah dibuat (tidak null)
        if (crawler != null) {
            // Hentikan proses crawling
            crawler.stop();

            // Update status summary menjadi STOPPED
            summary.setStatus(Status.STOPPED);
        }
    }

    /**
     * Even handler untuk tombol "Export"
     * 
     * Digunakan untuk mengeksport isi tabel ke berkas eksternal.
     */
    @FXML
    private void onExportClick() {
        // Pastikan proses sudah selesai
        Status status = summary.getStatus();

        // Pastikan ekspor hanya bisa dilakukan setelah proses selesai
        if (status != Status.STOPPED && status != Status.COMPLETED) {
            Application.openNotificationWindow("WARNING", "Export hanya bisa dilakukan setelah proses selesai.");
            return;
        }

        // Pastikan ekspor hanya bisa dilakukan jika data di tabel ada
        if (brokenLinks.isEmpty()) {
            Application.openNotificationWindow("WARNING", "Tidak ada data broken link untuk diexport.");
            return;
        }

        Application.openNotificationWindow("WARNING", "Fitur belum diimplementasikan.");
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

                    startBtn.getStyleClass().remove("active");
                    stopBtn.getStyleClass().remove("active");
                    exportBtn.getStyleClass().remove("active");
                }
                case CHECKING -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(false);
                    exportBtn.setDisable(true);

                    stopBtn.getStyleClass().remove("active");
                    exportBtn.getStyleClass().remove("active");

                    if (!startBtn.getStyleClass().contains("active")) {
                        startBtn.getStyleClass().add("active");
                    }
                }
                case STOPPED -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(false);
                    exportBtn.setDisable(false);

                    startBtn.getStyleClass().remove("active");

                    if (!stopBtn.getStyleClass().contains("active")) {
                        stopBtn.getStyleClass().add("active");
                    }
                }
                case COMPLETED -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    exportBtn.setDisable(false);

                    // hapus semua warna aktif
                    startBtn.getStyleClass().remove("active");
                    stopBtn.getStyleClass().remove("active");
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

    }

    // ============================= SUMMARY CARD =============================
    private void serSummaryCard() {
        // Label mengikuti nilai di Summary
        statusLabel.textProperty().bind(summary.statusProperty().asString());
        allLinksCountLabel.textProperty().bind(summary.totalLinksProperty().asString());
        webpageLinksCountLabel.textProperty().bind(summary.webpageLinksProperty().asString());
        brokenLinksCountLabel.textProperty().bind(summary.brokenLinksProperty().asString());

        summary.totalLinksProperty().bind(Bindings.size(allLinks));
        summary.webpageLinksProperty().bind(Bindings.size(webpageLinks));
        summary.brokenLinksProperty().bind(Bindings.size(brokenLinks));

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

        // kalau data berubah (misalnya karna filter), pagination reset
        brokenLinks.addListener((javafx.collections.ListChangeListener<Link>) c -> {
            currentPage = 1;
            updatePagination();
        });
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

        // Ambil subset data dari brokenLinks untuk ditampilkan di halaman saat ini
        currentPageData.setAll(brokenLinks.subList(fromIndex, toIndex));

        // Pasang subset data ini ke tabel
        brokenLinkTable.setItems(currentPageData);

        // Render ulang tombol navigasi (Prev, angka halaman, Next)
        renderPaginationButtons();
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

}
