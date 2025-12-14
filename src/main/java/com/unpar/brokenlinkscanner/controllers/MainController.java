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
 * Kelas ini bertugas untuk mengatur logika antarmuka pengguna, mengendalikan interaksi pengguna, serta mengelola data yang ditampilkan pada jendela utama.
 */
public class MainController implements LinkReceiver {
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

    // ========================= WEB CRAWLING =========================
    // Objek kelas Crawler untuk menjalankan proses crawling/pemeriksaan
    private Crawler crawler;
    // Objek kelas Summary untuk ringkasan proses crawling/pemeriksaan
    private final Summary summary = new Summary();
    // Menyimpan daftar daftar seluruh tautan
    private final ObservableList<Link> allLinks = FXCollections.observableArrayList();
    // Menyimpan daftar daftar tautan halaman
    private final FilteredList<Link> webpageLinks = new FilteredList<>(allLinks, link -> link.isWebpage() == true);
    // Menyimpan daftar daftar tautan rusak
    private final FilteredList<Link> brokenLinks = new FilteredList<>(allLinks, link -> !link.getError().isEmpty());

    // ========================= PAGINATION =========================
    // Menyimpan daftar tautan rusak benar-benar ditampilkan di TableView
    private final ObservableList<Link> paginationData = FXCollections.observableArrayList();
    // Jumlah baris per halaman pagination
    private static final int ROWS_PER_PAGE = 15;
    // Jumlah maksimal tombol halaman yang ditampilkan di pagination
    private static final int MAX_VISIBLE_PAGES = 5;
    // Halaman yang sedang aktif
    private int currentPage = 1;
    // Total halaman berdasarkan jumlah data
    private int totalPages = 1;

    // ========================= TITLE BAR =========================
    // Menyimpan posisi mouse di sumbu X (sudut kiri atas window)
    private double xOffset;
    // Menyimpan posisi mouse di sumbu Y (sudut kiri atas window)
    private double yOffset;

    /**
     * Method initialize otomatis dipanggil JavaFX setelah semua elemen FXML selesai dimuat.
     */
    @FXML
    public void initialize() {
        // Pasang global uncaught exception handler
        setupUncaughtExceptionHandler();

        // Jalankan setup UI di JavaFX Application Thread
        Platform.runLater(() -> {
            // Setup title bar
            setTitleBar();
            // Setup prilaku tombol
            setButtonState();
            // Setup tampilan ringkasan
            setSummaryCard();
            // Setup logika filter
            setFilterCard();
            // Setup tampilan TableView
            setTableView();
            // Setup logika pagination
            setPagination();

            // Inisialisasi crawler dan kirim MainController sebagai LinkReceiver
            crawler = new Crawler(this);
        });
    }

    /**
     * Method untuk menyimpan/menerima data tautan hasil crawling.
     *
     * @param link : objek Link yang dikirim oleh kelas Crawler
     */
    @Override
    public void receive(Link link) {
        /**
         * Karena crawling dijalankan bukan di thread JavaFX, maka di saat menambahkan tautan hasil crawling ke variabel internal harus menggunakan runLater().
         */
        Platform.runLater(() -> allLinks.add(link));
    }

    // ========================= EVENT HANDLER =========================

    /**
     * Event handler saat tombol Start ditekan.
     */
    @FXML
    private void onStartClick() {
        try {
            // Ambil seed URL dari input dan hilangkan spasi
            String seedUrl = seedUrlField.getText().trim();

            // Normalisasi URL (tanpa strict mode)
            String cleanedSeedUrl = UrlHandler.normalizeUrl(seedUrl, false);

            // Jika URL tidak valid, tampilkan peringatan
            if (cleanedSeedUrl == null) {
                showNotification("WARNING", "Please enter a valid seed URL before starting.");
                return;
            }

            // Update field (di GUI) dengan URL yang sudah dinormalisasi
            seedUrlField.setText(cleanedSeedUrl);

            // Bersihkan data lama
            allLinks.clear();

            // Set status menjadi CHECKING
            summary.setStatus(Status.CHECKING);

            // Jalankan crawling di virtual thread
            Thread.startVirtualThread(() -> {
                try {
                    // Catat waktu mulai
                    summary.setStartTime(System.currentTimeMillis());

                    // Mulai proses crawling
                    crawler.start(cleanedSeedUrl);

                    // Catat waktu selesai
                    summary.setEndTime(System.currentTimeMillis());

                    // Jika proses tidak dihentikan user
                    if (!crawler.isStoppedByUser()) {
                        // Update status menjadi COMPLETED
                        Platform.runLater(() -> summary.setStatus(Status.COMPLETED));
                    }
                } catch (Exception e) {
                    // Tampilkan error jika terjadi exception saat crawling
                    showNotification("ERROR", e.getMessage());
                }
            });
        } catch (Exception e) {
            // Tampilkan error tak terduga
            showNotification("ERROR", e.getMessage());
        }
    }

    /**
     * Event handler saat tombol Stop ditekan.
     */
    @FXML
    private void onStopClick() {
        try {
            // Pastikan crawler sudah diinisialisasi
            if (crawler != null) {

                // Hentikan proses crawling
                crawler.stop();

                // Update status menjadi STOPPED
                summary.setStatus(Status.STOPPED);
            }
        } catch (Exception e) {
            showNotification("ERROR", e.getMessage());
        }
    }

    /**
     * Event handler saat tombol Export ditekan.
     */
    @FXML
    private void onExportClick() {
        try {
            // Ambil status proses saat ini
            Status status = summary.getStatus();

            // Export hanya boleh saat status STOPPED atau COMPLETED
            if (status != Status.STOPPED && status != Status.COMPLETED) {
                showNotification("WARNING", "Export is only available after the process is finished.");
                return;
            }

            // Jika tidak ada broken link
            if (brokenLinks.isEmpty()) {
                showNotification("WARNING", "There are no broken links to export.");
                return;
            }

            // Buat file chooser
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Excel File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));

            // Tampilkan dialog simpan file
            File file = chooser.showSaveDialog(null);

            // Jika user batal
            if (file == null) {
                return;
            }

            // Pastikan ekstensi .xlsx
            if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                file = new File(file.getAbsolutePath() + ".xlsx");
            }

            File finalFile = file;

            // Jalankan export di virtual thread
            Thread.startVirtualThread(() -> {
                try {
                    // Buat exporter dengan data summary dan broken links
                    Exporter exporter = new Exporter(summary, brokenLinks);

                    // Simpan file Excel
                    exporter.save(finalFile);

                    // Tampilkan notifikasi sukses
                    showNotification("SUCCESS", "Data has been successfully exported to:\n" + finalFile.getAbsolutePath());
                } catch (Exception e) {
                    showNotification("ERROR", e.getMessage());
                }
            });
        } catch (Exception e) {
            showNotification("ERROR", e.getMessage());
        }
    }

    // ========================= SET UP GUI =========================

    /**
     * Method untuk mengatur prilaku title bar
     */
    private void setTitleBar() {
        // Ambil stage (window) dari scene title bar
        Stage stage = (Stage) titleBar.getScene().getWindow();

        // Event handler: kepanggil saat user klik mouse di title bar
        titleBar.setOnMousePressed((MouseEvent e) -> {
            // Ini penting biar pas drag, window nggak loncat ke posisi mouse, tapi tetap konsisten dari titik klik awal

            // Simpan posisi mouse relatif terhadap Scene untuk sumbu X
            xOffset = e.getSceneX();
            // Simpan posisi mouse relatif terhadap Scene untuk sumbu Y
            yOffset = e.getSceneY();
        });

        // Event handler: kepanggil saat mouse digeser sambil ditekan (drag)
        titleBar.setOnMouseDragged((MouseEvent e) -> {
            // e.getScreenX() dan e.getScreenY() itu posisi mouse dalam koordinat layar (global, terhadap monitor)

            // Geser window ke posisi X di layar
            stage.setX(e.getScreenX() - xOffset);
            // Geser window ke posisi Y di layar
            stage.setY(e.getScreenY() - yOffset);
        });


        // Event handler: kepanggil saat tombol minimize diklik
        minimizeBtn.setOnAction(e -> {
            // Sembunyikan jendela (jadi cuma icon)
            stage.setIconified(true);
        });

        // Event handler: kepanggil saat tombol maximize diklik
        maximizeBtn.setOnAction(e -> {
            // Fullscreen kalau sedang tidak fullscreen, dan tidak fullscreen kalau sedang fullscreen
            stage.setMaximized(!stage.isMaximized());
        });

        // Event handler: kepanggil saat tombol close diklik
        closeBtn.setOnAction(e -> {
            // Tutup jendela utama (keluarin seluruh aplikasi)
            stage.close();
        });
    }

    /**
     * Method untuk mengatur prilaku tombol Start, Stop dan Export berdasarkan status dari proses pemeriksaan.
     */
    private void setButtonState() {
        // Pasang listener ke atribut status dari objek Summary
        summary.statusProperty().addListener((obs, old, status) -> {
            switch (status) {
                // Tombol Start aktif, Stop dan Export inactive
                case IDLE -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    exportBtn.setDisable(true);
                }
                // Tombol Stop aktif, Start dan Export inactive
                case CHECKING -> {
                    startBtn.setDisable(true);
                    stopBtn.setDisable(false);
                    exportBtn.setDisable(true);
                }
                // Tombol Start dan Export active, Stop incative
                case STOPPED, COMPLETED -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    exportBtn.setDisable(false);
                }
            }
        });
    }

    /**
     * Method untuk mengatur konfigurasi TableView broken link, termasuk data, perilaku baris, dan tampilan sel.
     */
    private void setTableView() {
        // Atur agar hanya kolom terakhir yang fleksibel mengisi sisa lebar tabel
        brokenLinkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Set sumber data tabel ke daftar broken link
        brokenLinkTable.setItems(brokenLinks);

        // Binding kolom error ke property error milik Link
        errorColumn.setCellValueFactory(cell -> cell.getValue().errorProperty());

        // Binding kolom URL ke property URL milik Link
        urlColumn.setCellValueFactory(cell -> cell.getValue().urlProperty());

        // Custom row factory untuk menangani klik pada baris tabel
        brokenLinkTable.setRowFactory(tv -> {
            // Buat satu baris tabel
            TableRow<Link> row = new TableRow<>();

            // Event handler saat baris diklik
            row.setOnMouseClicked(event -> {
                // Pastikan baris tidak kosong dan diklik sekali
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    // Ambil data Link dari baris yang diklik
                    Link clickedLink = row.getItem();

                    // Buka jendela detail tautan
                    Application.openLinkWindow(clickedLink);
                }
            });

            // Kembalikan baris yang sudah dikonfigurasi
            return row;
        });

        // Custom cell factory untuk kolom error
        errorColumn.setCellFactory(col -> new TableCell<>() {
            /**
             * Dipanggil setiap kali isi sel berubah.
             */
            @Override
            protected void updateItem(String status, boolean empty) {
                // Panggil method parent
                super.updateItem(status, empty);

                // Jika sel kosong atau data null
                if (empty || status == null) {
                    // Kosongkan teks sel
                    setText(null);

                    // Reset style sel
                    setStyle("");
                } else {
                    // Ambil objek Link sesuai index baris
                    Link link = getTableView().getItems().get(getIndex());

                    // Ambil HTTP status code dari link
                    int code = link.getStatusCode();

                    // Tampilkan teks error
                    setText(status);

                    // Jika status code HTTP 4xx atau 5xx
                    if (code >= 400 && code < 600) {
                        // Tampilkan dengan warna abu gelap dan bold
                        setStyle("-fx-text-fill: -grey-dark; -fx-font-weight: bold;");
                    } else {
                        // Error non-HTTP (timeout, DNS, dll) dengan warna merah
                        setStyle("-fx-text-fill: -red; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Custom cell factory untuk kolom URL
        urlColumn.setCellFactory(col -> new TableCell<>() {
            // Hyperlink yang akan ditampilkan di dalam cell
            private final Hyperlink linkView = new Hyperlink();

            {
                // Event Handler : kepanggil saat hyperlink diklik
                linkView.setOnAction(e -> {
                    try {
                        // Buka URL di browser default OS
                        Desktop.getDesktop().browse(new URI(linkView.getText()));
                    } catch (Exception ex) {
                        // Tampilkan notifikasi jika gagal membuka browser
                        showNotification("ERROR", ex.getMessage());
                    }
                });
            }

            /**
             * Method ini dipanggil setiap kali isi cell berubah.
             */
            @Override
            protected void updateItem(String item, boolean empty) {
                // Panggil method parent
                super.updateItem(item, empty);

                // Jika cell kosong atau data null
                if (empty || item == null) {
                    // Hapus tampilan cell
                    setGraphic(null);
                } else {
                    // Set teks hyperlink dengan URL
                    linkView.setText(item);

                    // Tampilkan hyperlink di cell
                    setGraphic(linkView);
                }
            }
        });
    }

    /**
     * Method untuk menampilkan data ringkasan proses pemeriksaan.
     */
    private void setSummaryCard() {
        // Binding label status ke status summary
        statusLabel.textProperty().bind(summary.statusProperty().asString());

        // Binding label jumlah semua link
        allLinksCountLabel.textProperty().bind(summary.allLinksCountProperty().asString());

        // Binding label jumlah webpage link
        webpageLinksCountLabel.textProperty().bind(summary.webpageLinksCountProperty().asString());

        // Binding label jumlah broken link
        brokenLinksCountLabel.textProperty().bind(summary.brokenLinksCountProperty().asString());

        // Hitung jumlah semua link
        summary.allLinksCountProperty().bind(Bindings.size(allLinks));

        // Hitung jumlah webpage link
        summary.webpageLinksCountProperty().bind(Bindings.size(webpageLinks));

        // Hitung jumlah broken link
        summary.brokenLinksCountProperty().bind(Bindings.createIntegerBinding(
                // Hitung link yang memiliki error
                () -> (int) allLinks.stream().filter(l -> !l.getError().isEmpty()).count(),
                // Binding tergantung pada perubahan data di allLinks
                allLinks));

        // Listener untuk mengubah warna label status berdasarkan status
        summary.statusProperty().addListener((obs, old, status) -> {
            switch (status) {
                // Warna putih
                case IDLE -> statusLabel.setStyle("-fx-text-fill: #f9fafb;");
                // Warna biru
                case CHECKING -> statusLabel.setStyle("-fx-text-fill: #60a5fa;");
                // Warna merah
                case STOPPED -> statusLabel.setStyle("-fx-text-fill: #ef4444;");
                // Warna hijau
                case COMPLETED -> statusLabel.setStyle("-fx-text-fill: #10b981;");
            }
        });

        // Untuk trigger method setButtonState()
        summary.setStatus(Status.STOPPED);
        summary.setStatus(Status.IDLE);
    }

    // =============== FILTER ===============

    /**
     * Method untuk mengatur logika filter
     */
    private void setFilterCard() {
        // Listener: saat opsi filter URL berubah, jalankan ulang filter
        urlFilterOption.valueProperty().addListener((o, a, b) -> applyFilter());
        // Listener: saat teks URL filter berubah, jalankan ulang filter
        urlFilterField.textProperty().addListener((o, a, b) -> applyFilter());

        // Listener: saat opsi filter status code berubah, jalankan ulang filter
        statusCodeFilterOption.valueProperty().addListener((o, a, b) -> applyFilter());
        // Listener: saat teks status code berubah, jalankan ulang filter
        statusCodeFilterField.textProperty().addListener((o, a, b) -> applyFilter());
    }

    /**
     * Method untuk menerapkan filter pada daftar broken link berdasarkan kondisi URL dan status code.
     */
    private void applyFilter() {
        // Set predicate pada FilteredList brokenLinks
        brokenLinks.setPredicate(link -> {
            // Pastikan hanya link yang benar-benar error yang diproses
            if (link.getError().isEmpty()) {
                return false;
            }

            // Penentu hasil evaluasi filter URL
            boolean urlOk = true;
            // Ambil kondisi filter URL (Equals, Contains, dll)
            String urlCond = urlFilterOption.getValue();
            // Ambil teks pencarian URL dari input
            String urlText = urlFilterField.getText();
            // Jalankan filter URL hanya jika kondisi dan teks tersedia
            if (urlCond != null && !urlCond.isBlank() && urlText != null && !urlText.isBlank()) {
                // Ambil URL link dan ubah ke huruf kecil
                String u = link.getUrl().toLowerCase();
                // Ambil query filter dan ubah ke huruf kecil
                String q = urlText.toLowerCase();
                // Evaluasi kondisi URL sesuai pilihan pengguna
                urlOk = switch (urlCond) {
                    // URL harus sama persis
                    case "Equals" -> u.equals(q);
                    // URL mengandung teks query
                    case "Contains" -> u.contains(q);
                    // URL diawali dengan teks query
                    case "Starts With" -> u.startsWith(q);
                    // URL diakhiri dengan teks query
                    case "Ends With" -> u.endsWith(q);
                    // Default: lolos filter
                    default -> true;
                };
            }


            // Penentu hasil evaluasi filter status code
            boolean scOk = true;
            // Ambil kondisi filter status code
            String scCond = statusCodeFilterOption.getValue();
            // Ambil teks status code dari input
            String scText = statusCodeFilterField.getText();
            // Jalankan filter status code hanya jika input valid
            if (scCond != null && !scCond.isBlank() && scText != null && !scText.isBlank()) {
                try {
                    // Parsing input status code ke integer
                    int in = Integer.parseInt(scText.trim());
                    // Ambil status code milik link
                    int code = link.getStatusCode();
                    // Evaluasi kondisi status code
                    scOk = switch (scCond) {
                        // Status code harus sama persis
                        case "Equals" -> code == in;
                        // Status code lebih besar dari input
                        case "Greater Than" -> code > in;
                        // Status code lebih kecil dari input
                        case "Less Than" -> code < in;
                        // Default: lolos filter
                        default -> true;
                    };

                } catch (NumberFormatException ignore) {
                    // Jika input bukan angka, abaikan filter status code dan anggap kondisi status code lolos
                    scOk = true;
                }
            }

            // Link hanya ditampilkan jika lolos filter URL dan status code
            return urlOk && scOk;
        });
    }

    // =============== PAGINATION ===============

    /**
     * Method untuk mengatur mekanisme pagination awal dan memastikan pagination selalu diperbarui saat data broken link berubah.
     */
    private void setPagination() {
        // Panggil updatePagination pertama kali supaya tampilan tabel dan pagination langsung sinkron saat GUI baru dibuka
        updatePagination();

        /**
         * Pasang listener ke daftar brokenLinks.
         * Setiap kali isi brokenLinks berubah (bertambah/berkurang), pagination akan dihitung ulang.
         */
        brokenLinks.addListener((javafx.collections.ListChangeListener<Link>) c -> updatePagination());
    }

    /**
     * Method untuk menghitung ulang data pagination dan memperbarui tabel, tombol pagination, serta informasi halaman.
     */
    private void updatePagination() {
        // Ambil total jumlah baris data broken link
        int totalRows = brokenLinks.size();

        // Hitung total halaman dengan pembulatan ke atas
        totalPages = (int) Math.ceil((double) totalRows / ROWS_PER_PAGE);

        // Total page default adalah 1
        if (totalPages == 0) {
            totalPages = 1;
        }

        // Jika currentPage lebih besar dari totalPages (terjadi saat data berkurang), sesuaikan currentPage ke halaman terakhir
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        // Hitung index awal data untuk halaman saat ini
        int fromIndex = (currentPage - 1) * ROWS_PER_PAGE;

        // Hitung index akhir data, tapi jangan melebihi totalRows
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, totalRows);

        // Jika masih ada data
        if (totalRows > 0) {
            // Ambil subset data sesuai halaman aktif
            paginationData.setAll(brokenLinks.subList(fromIndex, toIndex));
        } else {
            // Jika tidak ada data, kosongkan paginationData
            paginationData.clear();
        }

        // Set data hasil pagination ke TableView
        brokenLinkTable.setItems(paginationData);

        // Render ulang tombol pagination (<< 1 2 3 >>)
        renderPaginationButtons();

        // Tentukan nomor item awal yang ditampilkan
        int itemStart = (totalRows == 0) ? 0 : fromIndex + 1;

        // Tentukan nomor item akhir yang ditampilkan
        int itemEnd = (totalRows == 0) ? 0 : toIndex;

        // Update label informasi jumlah halaman
        pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);

        // Update label informasi jumlah item
        itemInfoLabel.setText("Item " + itemStart + "-" + itemEnd + " of " + totalRows);
    }

    /**
     * Method untuk membuat dan menampilkan tombol-tombol pagination berdasarkan halaman aktif dan total halaman.
     */
    private void renderPaginationButtons() {
        // Hapus semua tombol pagination sebelumnya
        paginationBar.getChildren().clear();

        // ================= TOMBOL PREVIOUS =================

        // Buat tombol previous (<<)
        Button prevBtn = new Button("<<");

        // Tambahkan class CSS untuk styling
        prevBtn.getStyleClass().addAll("pagination-btn", "prev");
        // Disable tombol jika sedang di halaman pertama
        prevBtn.setDisable(currentPage <= 1);
        // Event handler : kepanggil saat user klik tombol previous
        prevBtn.setOnAction(e -> {
            // Pastikan masih bisa mundur
            if (currentPage > 1) {
                // Kurangi halaman aktif
                currentPage--;

                // Update ulang pagination
                updatePagination();
            }
        });
        // Tambahkan tombol previous ke pagination bar
        paginationBar.getChildren().add(prevBtn);


        // ================= HITUNG RANGE HALAMAN =================

        // Tentukan halaman awal yang akan ditampilkan
        int startPage = Math.max(1, currentPage - MAX_VISIBLE_PAGES / 2);

        // Tentukan halaman akhir yang akan ditampilkan
        int endPage = Math.min(startPage + MAX_VISIBLE_PAGES - 1, totalPages);

        // Jika jumlah halaman yang tampil masih kurang dari maksimal, geser startPage ke kiri agar jumlah tombol tetap konsisten
        if (endPage - startPage + 1 < MAX_VISIBLE_PAGES) {
            startPage = Math.max(1, endPage - MAX_VISIBLE_PAGES + 1);
        }

        // ================= TOMBOL NOMOR HALAMAN =================

        // Loop untuk membuat tombol nomor halaman
        for (int i = startPage; i <= endPage; i++) {
            // Buat tombol dengan label nomor halaman
            Button pageBtn = new Button(String.valueOf(i));

            // Tambahkan class CSS pagination
            pageBtn.getStyleClass().add("pagination-btn");

            // Jika tombol ini adalah halaman aktif
            if (i == currentPage) {
                // Tambahkan class active untuk styling
                pageBtn.getStyleClass().add("active");
            }

            // Simpan nilai halaman ke variabel final
            final int pageIndex = i;

            // Event handler saat tombol halaman ditekan
            pageBtn.setOnAction(e -> {
                // Set halaman aktif ke halaman yang diklik
                currentPage = pageIndex;

                // Update ulang pagination
                updatePagination();
            });

            // Tambahkan tombol halaman ke pagination bar
            paginationBar.getChildren().add(pageBtn);
        }

        // ================= TOMBOL NEXT =================

        // Buat tombol next (>>)
        Button nextBtn = new Button(">>");

        // Tambahkan class CSS untuk styling
        nextBtn.getStyleClass().addAll("pagination-btn", "next");

        // Disable tombol jika sudah di halaman terakhir
        nextBtn.setDisable(currentPage >= totalPages);

        // Event handler saat tombol next ditekan
        nextBtn.setOnAction(e -> {
            // Pastikan masih bisa maju
            if (currentPage < totalPages) {
                // Tambah halaman aktif
                currentPage++;

                // Update ulang pagination
                updatePagination();
            }
        });

        // Tambahkan tombol next ke pagination bar
        paginationBar.getChildren().add(nextBtn);
    }

    // ========================= UTILS =========================

    /**
     * Method untuk menampilkan window notifikasi.
     */
    private void showNotification(String type, String message) {
        // Gunakan pesan default jika null atau kosong
        String msg = (message == null || message.isBlank()) ? "Unknown error." : message;

        // Buka window notifikasi
        Platform.runLater(() -> Application.openNotificationWindow(type, msg));
    }

    /**
     * Method untuk menangani exception yang tidak tertangkap di thread mana pun.
     */
    private void setupUncaughtExceptionHandler() {
        // Set handler global untuk semua thread
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Ambil pesan exception
            String message = throwable.getMessage();

            // Jika pesan kosong, pakai toString()
            if (message == null || message.isBlank()) {
                message = throwable.toString();
            }

            // Tampilkan sebagai notifikasi error
            showNotification("ERROR", message);
        });
    }
}
