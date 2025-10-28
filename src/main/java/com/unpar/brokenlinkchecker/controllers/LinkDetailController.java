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

    @FXML
    private TextField urlField;
    @FXML
    private TextField finalUrlField;
    @FXML
    private TextField contentTypeField;
    @FXML
    private TextField statusField;
    @FXML
    private TableView<Map.Entry<Link, String>> sourceTable;
    @FXML
    private TableColumn<Map.Entry<Link, String>, String> anchorColumn;
    @FXML
    private TableColumn<Map.Entry<Link, String>, String> sourceColumn;
    @FXML
    private Button closeBtn;

    @FXML
    private void initialize() {
        closeBtn.setOnAction(e -> ((Stage) closeBtn.getScene().getWindow()).close());
    }

    public void setLink(Link link) {
        // isi field utama
        urlField.setText(link.getUrl());
        finalUrlField.setText(link.getFinalUrl());
        contentTypeField.setText(link.getContentType());
        statusField.setText(link.getError());


        // isi tabel source
        sourceTable.setItems(FXCollections.observableArrayList(link.getConnection().entrySet()));

        // Kolom anchor text
        anchorColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getValue()));

        // Kolom Webpage URL
        sourceColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getKey().getUrl()));

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
}
