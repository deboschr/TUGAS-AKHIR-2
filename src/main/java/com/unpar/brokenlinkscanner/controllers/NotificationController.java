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
 * Controller buat window notifikasi.
 */
public class NotificationController {
    @FXML
    private BorderPane root;
    @FXML
    private HBox titleBar;
    @FXML
    private Button closeBtn;
    @FXML
    private Label titleLabel, iconLabel, messageLabel;

    // Variabel buat nyimpen posisi mouse pas mulai drag window
    private double xOffset;
    private double yOffset;

    @FXML
    private void initialize() {
        setTitleBar();
    }

    /**
     * Method buat nyiapin fungsi title bar, supaya:
     * - window bisa digeser dengan drag,
     * - tombol close bisa nutup window.
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
     * Method buat menetapkan nilai yang mau di tampilin di window
     *
     * @param type    Jenis notifikasi: "ERROR", "WARNING", "INFO", atau "SUCCESS"
     * @param message Pesan yang mau ditampilin
     */
    public void setNotification(String type, String message) {
        // Set pesan notifikasi
        messageLabel.setText(message);
        // Ubah tipe jadi huruf besar
        type = type.toUpperCase();

        switch (type) {
            case "ERROR" -> applyStyle("#dc2626", "\u2716", "ERROR");
            case "WARNING" -> applyStyle("#f59e0b", "\u26A0", "WARNING");
            case "INFO" -> applyStyle("#3b82f6", "\u2139", "INFORMATION");
            case "SUCCESS" -> applyStyle("#10b981", "\u2714", "SUCCESS");
            default -> applyStyle("#6b7280", "\u2753", "UNKNOWN");
        }
    }

    /**
     * Method buat ngatur tampilan warna dan ikon notifikasi.
     *
     * @param color Warna utama notifikasi (pakai format hex)
     * @param icon  Unicode ikon yang mau ditampilin
     * @param title Teks judul yang mau ditulis di header
     */
    private void applyStyle(String color, String icon, String title) {
        // Set judul notifikasi
        titleLabel.setText(title);

        // Atur warna backgroud dari title bar biar sesuai sama tipe notifikasi
        titleBar.setStyle("-fx-background-color: " + color + ";");

        // Set icon notifikasi
        iconLabel.setText(icon);

        // Atur warna icon biar sama kaya warna title bar
        iconLabel.setStyle("-fx-text-fill: " + color + ";");

        String existing = root.getStyle();
        root.setStyle(existing + "-fx-border-color: " + color + ";");

    }
}
