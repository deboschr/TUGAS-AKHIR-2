package com.unpar.brokenlinkchecker.controllers;

import com.unpar.brokenlinkchecker.models.Link;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.Map;

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

    private final ObservableList<Map.Entry<Link, String>> webpageLinks = FXCollections.observableArrayList();
    private double xOffset;
    private double yOffset;

    @FXML
    private void initialize() {
        setTitleBar();
    }

    /**
     * Method buat mengatur interaksi pada title bar (drag window dan close button).
     */
    private void setTitleBar() {
        /*
         * Platform.runLater dipakai biar kode di dalamnya jalan setelah UI (scene)
         * bener-bener siap dan udah ditampilin.
         */
        Platform.runLater(() -> {
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

            // Tutup window saat tombol close diklik
            closeBtn.setOnAction(e -> stage.close());
        });
    }

    /**
     * Method buat menetapkan data dari objek Link yang akan ditampilkan di window.
     * 
     * @param link Objek Link yang akan ditampilkan
     */
    public void setLink(Link link) {
        // Set value dari field
        urlField.setText(link.getUrl());
        finalUrlField.setText(link.getFinalUrl());
        contentTypeField.setText(link.getContentType());
        errorField.setText(link.getError());

        // Set isi list webpage links
        webpageLinks.setAll(link.getConnection().entrySet());

        makeFieldClickable(urlField);
        makeFieldClickable(finalUrlField);

        // Siapkan tampilan tabel
        setTableView();
    }

    /**
     * Method buat ngatur konfigurasi tabel.
     */
    private void setTableView() {

        // Biar kolom terakhir selalu memenuhi ukuan tabel sisa
        webpageLinkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Set value dari kolom
        anchorTextColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getValue()));
        webpageUrlColumn
                .setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey().getUrl()));

        // Hubungkan data (ObservableList) ke tabel
        webpageLinkTable.setItems(webpageLinks);

        /*
         * Mengatur kolom URL biar ditampilkan sebagai hyperlink.
         * 
         * Pake cell factory karena kita mau mengganti tampilan default cell
         * (yang biasanya cuma teks biasa) menjadi komponen hyperlink.
         */
        webpageUrlColumn.setCellFactory(col -> new TableCell<>() {
            // Komponen Hyperlink untuk setiap sel URL
            private final Hyperlink linkView = new Hyperlink();

            /*
             * Pake initializer block biar isi didalamnya dijalankan satu kali
             * saat objek kelas TableCell dibuat. Intinya, blok ini dipakai buat
             * inisialisasi awal, mirip kaya constructor di kelas biasa.
             * 
             * Bikin even handler, kalau hyperlink di klik maka kita buka URLnya
             * di browser bawaan OS.
             */
            {
                linkView.setOnAction(e -> {
                    try {
                        Desktop.getDesktop().browse(new URI(linkView.getText()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            /**
             * Method bawaan TableCell buat memperbarui isi tampilan cell.
             * Dipanggil setiap kali data di baris berubah.
             * 
             * @param item  nilai baru yang mau ditampilkan di cell
             * @param empty true kalau cell sedang kosong
             */
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    // Kalau cell kosong, hapus komponen tampilan
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
     * Method buat bikin TextField bisa di klik dan membuka URL di browser.
     * 
     * @param field Field teks yang menampilkan URL.
     */
    private void makeFieldClickable(TextField field) {
        // Tambahkan event klik untuk membuka URL di browser default
        field.setOnMouseClicked(e -> {
            String url = field.getText(); // ambil isi teks (URL) dari field

            if (url != null && !url.isEmpty()) { // pastikan tidak kosong
                try {
                    Desktop.getDesktop().browse(new URI(url)); // buka di browser
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Ubah warna teks jadi biru dan kursor jadi tangan (pointer)
        field.setStyle("-fx-text-fill: #19539a; -fx-cursor: hand;");
    }
}
