package com.unpar.brokenlinkchecker.controllers;

import com.unpar.brokenlinkchecker.models.Link;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.Map;

public class LinkDetailController {

    @FXML private TextField urlField;
    @FXML private TextField finalUrlField;
    @FXML private TextField contentTypeField;
    @FXML private TextField statusField;
    @FXML private TableView<Map.Entry<Link, String>> sourceTable;
    @FXML private TableColumn<Map.Entry<Link, String>, String> anchorColumn;
    @FXML private TableColumn<Map.Entry<Link, String>, String> sourceColumn;
    @FXML private Button closeBtn;

    @FXML
    private void initialize() {
        closeBtn.setOnAction(e -> ((Stage) closeBtn.getScene().getWindow()).close());
    }

    public void setLink(Link link) {
        // isi field utama
        urlField.setText(link.getUrl());
        finalUrlField.setText(link.getFinalUrl());
        contentTypeField.setText(link.getContentType());

        // status dengan warna sesuai kategori
        int code = link.getStatusCode();
        if (code >= 500) {
            statusField.setText(link.getError());
            statusField.getStyleClass().add("status-server-error");
        } else if (code >= 400) {
            statusField.setText(link.getError());
            statusField.getStyleClass().add("status-client-error");
        } else {
            statusField.setText(link.getError().isEmpty()
                    ? String.valueOf(link.getStatusCode())
                    : link.getError());
            statusField.getStyleClass().add("status-ok");
        }

        // isi tabel source
        sourceTable.setItems(FXCollections.observableArrayList(link.getConnection().entrySet()));

        anchorColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getValue())); // anchor text

        sourceColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getKey().getUrl())); // source URL

        // hyperlink di kolom source
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
}
