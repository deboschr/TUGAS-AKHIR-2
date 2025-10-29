package com.unpar.brokenlinkchecker.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.application.Platform;

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
        initTitleBar();
    }

    private void initTitleBar() {
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
     * @param type "ERROR" atau "WARNING"
     * @param message pesan yang akan ditampilkan
     */
    public void setNotification(String type, String message) {
        messageLabel.setText(message);

        if (type.equalsIgnoreCase("ERROR")) {
            titleLabel.setText("ERROR");
            titleBar.setStyle("-fx-background-color: #dc2626;"); // merah
            iconLabel.setText("\u2716");
            iconLabel.setStyle("-fx-text-fill: #dc2626;");
        } else {
            titleLabel.setText("WARNING");
            titleBar.setStyle("-fx-background-color: #f59e0b;"); // oranye
            iconLabel.setText("\u26A0");
            iconLabel.setStyle("-fx-text-fill: #f59e0b;");
        }
    }
}
