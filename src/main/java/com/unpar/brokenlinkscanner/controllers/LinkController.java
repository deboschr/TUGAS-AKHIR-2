package com.unpar.brokenlinkscanner.controllers;

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

    private double xOffset;
    private double yOffset;

    private final Link link;

    public LinkController(Link link) {
        this.link = link;
    }

    @FXML
    private void initialize() {
        setTitleBar();
        setFieldValue();
        setTableValue();
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

    public void setFieldValue() {

        urlField.setText(link.getUrl());
        finalUrlField.setText(link.getFinalUrl());
        contentTypeField.setText(link.getContentType());
        errorField.setText(link.getError());

        makeFieldClickable(urlField);
        makeFieldClickable(finalUrlField);
    }

    private void setTableValue() {

        webpageLinkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        webpageLinkTable.setItems(FXCollections.observableArrayList(link.getWebpageSources().entrySet()));
        anchorTextColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getValue()));
        webpageUrlColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey().getUrl()));

        setupHyperlinkColumn(webpageUrlColumn);
    }

    private void makeFieldClickable(TextField field) {

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


        field.setStyle("-fx-text-fill: #19539a; -fx-cursor: hand;");
    }

    private void setupHyperlinkColumn(TableColumn<Map.Entry<Link, String>, String> column) {

        column.setCellFactory(col -> new TableCell<>() {


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

                    setGraphic(linkView);
                }
            }
        });

    }

}
