package com.unpar.brokenlinkscanner.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;


public class NotifController {
    @FXML
    private BorderPane root;
    @FXML
    private HBox titleBar;
    @FXML
    private Button closeBtn;
    @FXML
    private Label titleLabel, iconLabel, messageLabel;


    private double xOffset;
    private double yOffset;

    private final String type;
    private final String message;

    public NotifController(String type, String message) {
        this.type = type.toUpperCase();
        this.message = message;
    }

    @FXML
    private void initialize() {
        setTitleBar();
        setNotification();
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

    public void setNotification() {

        messageLabel.setText(message);

        switch (type) {
            case "ERROR" -> applyStyle("-red", "\u2716", "ERROR");
            case "WARNING" -> applyStyle("-orange", "\u26A0", "WARNING");
            case "SUCCESS" -> applyStyle("-green", "\u2714", "SUCCESS");
            default -> applyStyle("-grey-light", "\u2753", "UNKNOWN");
        }
    }

    private void applyStyle(String color, String icon, String title) {

        titleLabel.setText(title);

        titleBar.setStyle("-fx-background-color: " + color + ";");

        iconLabel.setText(icon);

        iconLabel.setStyle("-fx-text-fill: " + color + ";");

        String existing = root.getStyle();

        root.setStyle(existing + "-fx-border-color: " + color + ";");
    }
}
