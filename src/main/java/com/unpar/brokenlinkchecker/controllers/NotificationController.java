package com.unpar.brokenlinkchecker.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class NotificationController {

    @FXML
    private HBox titleBar;
    @FXML
    private Button closeBtn;
    @FXML
    private Label titleLabel, iconLabel, messageLabel;

    private double xOffset;
    private double yOffset;

    @FXML
    private void initialize() {
        setTitleBar();
    }

    private void setTitleBar() {
        Platform.runLater(() -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();

            titleBar.setOnMousePressed((MouseEvent e) -> {
                xOffset = e.getSceneX();
                yOffset = e.getSceneY();
            });

            titleBar.setOnMouseDragged((MouseEvent e) -> {
                stage.setX(e.getScreenX() - xOffset);
                stage.setY(e.getScreenY() - yOffset);
            });

            closeBtn.setOnAction(e -> stage.close());
        });
    }

    /**
     * Menampilkan notifikasi dengan tipe dan pesan yang sesuai.
     *
     * @param type    Jenis notifikasi: "ERROR", "WARNING", "INFO", atau "SUCCESS"
     * @param message Pesan yang akan ditampilkan
     */
    public void setNotification(String type, String message) {
        messageLabel.setText(message);
        type = type.toUpperCase();

        switch (type) {
            case "ERROR" -> applyStyle("#dc2626", "\u2716", "ERROR"); // merah, ✖
            case "WARNING" -> applyStyle("#f59e0b", "\u26A0", "WARNING"); // oranye, ⚠
            case "INFO" -> applyStyle("#3b82f6", "\u2139", "INFORMATION"); // biru, ℹ
            case "SUCCESS" -> applyStyle("#10b981", "\u2714", "SUCCESS"); // hijau, ✔
            default -> applyStyle("#6b7280", "\u2753", "UNKNOWN"); // abu-abu, ❓
        }
    }

    /**
     * Terapkan warna dan ikon berdasarkan tipe notifikasi.
     */
    private void applyStyle(String color, String icon, String title) {
        titleLabel.setText(title);
        titleBar.setStyle("-fx-background-color: " + color + ";");
        iconLabel.setText(icon);
        iconLabel.setStyle("-fx-text-fill: " + color + ";");
    }
}
