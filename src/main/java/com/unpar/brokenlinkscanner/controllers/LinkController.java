package com.unpar.brokenlinkscanner.controllers;

import com.unpar.brokenlinkscanner.Application;
import com.unpar.brokenlinkscanner.models.Link;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.Map;

/**
 * Kelas ini bertugas untuk mengatur logika antarmuka pengguna, mengendalikan interaksi pengguna, serta mengelola data yang ditampilkan pada jendela detail tautan.
 */
public class LinkController {
    @FXML
    private HBox titleBar;
    @FXML
    private Button closeBtn;
    @FXML
    private TextField urlField, finalUrlField, contentTypeField, errorField;
    @FXML
    private TableView<Map.Entry<Link, String>> webpageLinkTable;
    @FXML
    private TableColumn<Map.Entry<Link, String>, String> anchorTextColumn, webpageUrlColumn;

    // Menyimpan posisi mouse di sumbu X (sudut kiri atas window)
    private double xOffset;
    // Menyimpan posisi mouse di sumbu Y (sudut kiri atas window)
    private double yOffset;

    // Objek Link yang detailnya akan ditampilkan di jendela ini
    private final Link link;

    /**
     * Link dikirim dari MainController melalui ControllerFactory di Application.
     *
     * @param link : objek Link yang ingin ditampilkan detailnya
     */
    public LinkController(Link link) {
        this.link = link;
    }

    /**
     * Method initialize otomatis dipanggil JavaFX setelah semua elemen FXML selesai dimuat.
     */
    @FXML
    private void initialize() {
        // Jalankan setup GUI di JavaFX Application Thread
        Platform.runLater(() -> {
            // Setup title bar
            setTitleBar();
            // Isi TextField dengan data dari objek Link
            setFieldValue();
            // Isi TableView dengan daftar halaman sumber
            setTableValue();
        });
    }

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

        // Event handler: kepanggil pas tombol close diklik
        closeBtn.setOnAction(e -> {
            // Tutup Stage saat ini (window detail), bukan keluarin seluruh aplikasi
            stage.close();
        });
    }

    /**
     * Method untuk mengisi nilai TextField dengan data dari objek Link.
     */
    public void setFieldValue() {
        // Tampilkan URL asli
        urlField.setText(link.getUrl());

        // Tampilkan final URL (setelah redirect)
        finalUrlField.setText(link.getFinalUrl());

        // Tampilkan content type hasil response
        contentTypeField.setText(link.getContentType());

        // Tampilkan pesan error (jika ada)
        errorField.setText(link.getError());

        // Buat field URL bisa diklik dan dibuka di browser
        makeFieldClickable(urlField);

        // Buat field final URL juga bisa diklik
        makeFieldClickable(finalUrlField);
    }

    /**
     * Method untuk mengisi data ke dalam TableView dari halaman sumber (webpageSources).
     */
    private void setTableValue() {
        // Atur agar kolom terakhir menyesuaikan lebar tabel
        webpageLinkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Ambil data sumber halaman dari objek Link dan masukkan ke tabel
        webpageLinkTable.setItems(FXCollections.observableArrayList(link.getWebpageSources().entrySet()));

        // Set nilai kolom anchor text
        anchorTextColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getValue()));

        // Set nilai kolom URL halaman sumber
        webpageUrlColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey().getUrl()));

        // Ubah isi kolom URL menjadi hyperlink
        setupHyperlinkColumn(webpageUrlColumn);
    }

    /**
     * Method untuk membuat TextField bersifat clickable seperti hyperlink.
     *
     * @param field TextField yang ingin dibuat clickable
     */
    private void makeFieldClickable(TextField field) {
        // Event Handler : kepanggil saat user klik field
        field.setOnMouseClicked(e -> {
            // Ambil teks URL dari field
            String url = field.getText();

            // Pastikan URL tidak null dan tidak kosong
            if (url != null && !url.isEmpty()) {
                try {
                    // Buka URL menggunakan browser default OS
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    // Print error jika gagal membuka browser
                    ex.printStackTrace();
                }
            }
        });

        // Ubah tampilan text agar terlihat seperti link
        field.setStyle("-fx-text-fill: #19539a; -fx-cursor: hand;");
    }

    /**
     * Method untuk mengubah kolom TableView menjadi hyperlink yang bisa diklik.
     *
     * @param column kolom TableView yang akan dijadikan hyperlink
     */
    private void setupHyperlinkColumn(TableColumn<Map.Entry<Link, String>, String> column) {
        // Set cell factory untuk kolom
        column.setCellFactory(col -> new TableCell<>() {
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
                        Application.openNotificationWindow("ERROR", ex.getMessage());
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

                // Kalau cell kosong atau data null
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
}
