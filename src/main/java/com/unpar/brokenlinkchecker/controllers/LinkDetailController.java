package com.unpar.brokenlinkchecker.controllers;

import com.unpar.brokenlinkchecker.models.Link;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.Map;

public class LinkDetailController {
    @FXML
    private HBox titleBar;
    @FXML
    private Button closeBtn;
    @FXML
    private TextField urlField, finalUrlField, contentTypeField, statusField;
    @FXML
    private TableView<Map.Entry<Link, String>> sourceTable;
    @FXML
    private TableColumn<Map.Entry<Link, String>, String> anchorColumn, sourceColumn;

    private final ObservableList<Map.Entry<Link, String>> webpageLinks = FXCollections.observableArrayList();
    private double xOffset;
    private double yOffset;

    @FXML
    private void initialize() {
        initTitleBar();
        makeUrlClickable(urlField);
        makeUrlClickable(finalUrlField);
    }

    // ============================= TITLE BAR =============================
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


    public void setLink(Link link) {
        // isi field utama
        urlField.setText(link.getUrl());
        finalUrlField.setText(link.getFinalUrl());
        contentTypeField.setText(link.getContentType());
        statusField.setText(link.getError());


        // isi tabel source
        webpageLinks.setAll(link.getConnection().entrySet());
        sourceTable.setItems(webpageLinks);

        // Kolom anchor text
        anchorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getValue()));

        // Kolom Webpage URL
        sourceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey().getUrl()));

        // hyperlink di kolom Webpage URL
        sourceColumn.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink linkView = new Hyperlink();

            {
                linkView.setOnAction(e -> {
                    try {
                        Desktop.getDesktop().browse(new URI(linkView.getText()));
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
                    linkView.setText(item);
                    linkView.setStyle("-fx-text-fill: #60a5fa;");
                    setGraphic(linkView);
                }
            }
        });
    }

    private void makeUrlClickable(TextField field) {
        field.setOnMouseClicked(e -> {
            String url = field.getText();
            if (url != null && !url.isEmpty()) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        field.setStyle("-fx-text-fill: #60a5fa; -fx-cursor: hand;");
    }
}
