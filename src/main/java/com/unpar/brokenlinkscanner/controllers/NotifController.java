package com.unpar.brokenlinkscanner.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Kelas ini bertugas untuk mengatur logika antarmuka pengguna, mengendalikan interaksi pengguna, serta mengelola data yang ditampilkan pada jendela notifikasi.
 */
public class NotifController {
    @FXML
    private BorderPane root;
    @FXML
    private HBox titleBar;
    @FXML
    private Button closeBtn;
    @FXML
    private Label titleLabel, iconLabel, messageLabel;

    // Menyimpan posisi mouse di sumbu X (sudut kiri atas window)
    private double xOffset;
    // Menyimpan posisi mouse di sumbu Y (sudut kiri atas window)
    private double yOffset;

    // Tipe notifikasi (ERROR, WARNING, SUCCESS)
    private final String type;
    // Pesan notifikasi yang akan ditampilkan ke pengguna
    private final String message;

    /**
     * type dan message dikirim dari MainController melalui ControllerFactory di Application.
     *
     * @param type    tipe notifikasi (error, warning, success)
     * @param message pesan yang akan ditampilkan
     */
    public NotifController(String type, String message) {
        this.type = type.toUpperCase();
        this.message = message;
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
            // Set nilai dan tampilan notifikasi sesuai tipe
            setNotifValue();
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
     * Method untuk mengatur isi dan tampilan notifikasi berdasarkan tipe notifikasi.
     */
    public void setNotifValue() {
        // Set teks pesan notifikasi
        messageLabel.setText(message);

        // Tentukan style notifikasi berdasarkan tipe
        switch (type) {
            // Notifikasi error (merah, ikon silang)
            case "ERROR" -> applyStyle("-red", "\u2716", "ERROR");

            // Notifikasi warning (oranye, ikon peringatan)
            case "WARNING" -> applyStyle("-orange", "\u26A0", "WARNING");

            // Notifikasi sukses (hijau, ikon centang)
            case "SUCCESS" -> applyStyle("-green", "\u2714", "SUCCESS");

            // Default kalau tipe tidak dikenali (abu-abu, ikon tanda tanya)
            default -> applyStyle("-grey-light", "\u2753", "UNKNOWN");
        }
    }

    /**
     * Method untuk menerapkan style ke window notifikasi.
     *
     * @param color : warna utama notifikasi
     * @param icon : ikon simbol notifikasi
     * @param title : tipe notifikasi
     */
    private void applyStyle(String color, String icon, String title) {
        // Set tipe notifikasi menjadi judul notifikasi
        titleLabel.setText(title);

        // Set warna background title bar sesuai tipe notifikasi
        titleBar.setStyle("-fx-background-color: " + color + ";");

        // Set ikon notifikasi
        iconLabel.setText(icon);

        // Set warna ikon sesuai tipe notifikasi
        iconLabel.setStyle("-fx-text-fill: " + color + ";");

        // Ambil style root yang sudah ada (kalau ada)
        String existingStyle = root.getStyle();

        // Tambahkan border dengan warna sesuai tipe notifikasi
        root.setStyle(existingStyle + "-fx-border-color: " + color + ";");
    }
}
