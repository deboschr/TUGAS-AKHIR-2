package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.LinkItem;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import javafx.event.ActionEvent;

/**
 * Controller untuk file table.fxml
 *
 * Tugasnya: mengatur interaksi UI seperti pengisian data ke tabel,
 * respon tombol export, dan tombol pagination.
 */
public class ControllerTable {

   @FXML
   private TableView<LinkItem> resultTable;

   @FXML
   private TableColumn<LinkItem, String> statusColumn;

   @FXML
   private TableColumn<LinkItem, String> urlColumn;

   @FXML
   private Button exportButton;

   // ObservableList: koleksi data dinamis yang langsung update ke UI TableView
   private final ObservableList<LinkItem> data = FXCollections.observableArrayList();

   /**
    * Dipanggil otomatis setelah FXML selesai dimuat.
    * Cocok untuk inisialisasi kolom tabel, data dummy, dll.
    */
   @FXML
   private void initialize() {
      // Binding antara kolom dan field di model LinkItem
      statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
      urlColumn.setCellValueFactory(cellData -> cellData.getValue().urlProperty());

      // Tambahkan data dummy (buat latihan)
      data.addAll(
              new LinkItem("200 OK", "https://unpar.ac.id/"),
              new LinkItem("404 Not Found", "https://unpar.ac.id/invalid-page"),
              new LinkItem("301 Moved Permanently", "https://old.unpar.ac.id/"),
              new LinkItem("500 Internal Server Error", "https://unpar.ac.id/error"),
              new LinkItem("403 Forbidden", "https://unpar.ac.id/private")
      );

      // Masukkan data ke tabel
      resultTable.setItems(data);
   }

   /**
    * Event handler tombol Export
    */
   @FXML
   private void onExportClick(ActionEvent event) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Export");
      alert.setHeaderText(null);
      alert.setContentText("Fitur export belum diimplementasikan 😄");
      alert.showAndWait();
   }
}
