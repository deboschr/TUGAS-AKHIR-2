package com.unpar.brokenlinkscanner;

import com.unpar.brokenlinkscanner.controllers.LinkController;
import com.unpar.brokenlinkscanner.controllers.NotifController;
import com.unpar.brokenlinkscanner.models.Link;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

/**
 * Kelas utama aplikasi JavaFX dan bertugas untuk membuka jendela aplikasi.
 */
public class Application extends javafx.application.Application {
    // Menyimpan reference ke stage utama aplikasi Supaya bisa dipakai lagi pas buka window lain
    private static Stage MAIN_STAGE;

    /**
     * Method start adalah method pertama yang dipanggil JavaFX saat aplikasi dijalankan.
     *
     * @param stage : stage utama yang disediakan oleh JavaFX
     */
    @Override
    public void start(Stage stage) {
        // Simpan stage utama ke variabel static
        MAIN_STAGE = stage;

        // Langsung buka jendela utama aplikasi
        openMainWindow();
    }

    /**
     * Method main sebagai entry point Java,
     * tapi di JavaFX isinya cuma manggil launch().
     */
    public static void main(String[] args) {
        // Menjalankan lifecycle JavaFX
        launch();
    }

    /**
     * Method untuk membuka jendela utama aplikasi.
     */
    public static void openMainWindow() {
        try {
            // Mendapatkan alamat file FXML jendela utama
            URL fxml = Application.class.getResource("/com/unpar/brokenlinkscanner/scenes/main-scene.fxml");

            // Buat FXMLLoader dengan file FXML tersebut
            FXMLLoader loader = new FXMLLoader(fxml);

            // Load FXML dan bungkus hasilnya ke dalam Scene
            Scene scene = new Scene(loader.load());

            // Pasang scene ke stage utama
            MAIN_STAGE.setScene(scene);
            // Hilangkan dekorasi bawaan OS (title bar dan tombol close/minimize/maximize)
            MAIN_STAGE.initStyle(StageStyle.UNDECORATED);
            // Posisikan jendela ke tengah layar
            MAIN_STAGE.centerOnScreen();
            // Set jendela agar langsung fullscreen
            MAIN_STAGE.setMaximized(true);
            // Set minimum lebar dan tinggi jendela
            MAIN_STAGE.setMinWidth(1024);
            MAIN_STAGE.setMinHeight(600);
            // Tampilkan jendela detail tautan
            MAIN_STAGE.show();
        } catch (Exception e) {
            // Print error ke console
            e.printStackTrace();
        }
    }

    /**
     * Method untuk membuka jendela detail tautan.
     *
     * @param link : objek Link yang mau ditampilkan detailnya
     */
    public static void openLinkWindow(Link link) {
        try {
            // Ambil file FXML untuk jendela detail tautan
            URL fxml = Application.class.getResource("/com/unpar/brokenlinkscanner/scenes/link-scene.fxml");

            // Buat FXMLLoader untuk load FXML
            FXMLLoader loader = new FXMLLoader(fxml);

            /**
             * Set controller factory supaya kita bisa inject parameter (Link) ke LinkController constructor.
             */
            loader.setControllerFactory(param -> {
                // Buat LinkController dengan constructor yang menerima parameter objek Link
                return new LinkController(link);
            });

            // Load FXML dan bungkus ke Scene
            Scene scene = new Scene(loader.load());

            // Buat stage baru untuk jendela detail tautan
            Stage stage = new Stage();

            // Pasang scene ke stage
            stage.setScene(scene);
            // Hilangkan dekorasi bawaan OS (title bar dan tombol close/minimize/maximize)
            stage.initStyle(StageStyle.UNDECORATED);
            // Set induk/owner dari jendela detail tautan
            stage.initOwner(MAIN_STAGE);
            // Set window sebagai modal (biar user tidak bisa klik jendela lain)
            stage.initModality(Modality.WINDOW_MODAL);
            // Posisikan jendela ke tengah layar
            stage.centerOnScreen();
            // Tampilkan jendela detail tautan
            stage.show();
        } catch (Exception e) {
            // Print error ke console
            e.printStackTrace();
        }
    }

    /**
     * Method untuk membuka jendela notifikasi.
     *
     * @param type    : tipe notifikasi (error / warning / success)
     * @param message : isi pesan notifikasi
     */
    public static void openNotificationWindow(String type, String message) {
        try {
            // Ambil file FXML untuk jendela notifikasi
            URL fxml = Application.class.getResource("/com/unpar/brokenlinkscanner/scenes/notif-scene.fxml");

            // Buat FXMLLoader untuk load FXML
            FXMLLoader loader = new FXMLLoader(fxml);

            /**
             * Set controller factory supaya kita bisa inject parameter (type, message) ke NotifController constructor.
             */
            loader.setControllerFactory(param -> {
                // Buat NotifController dengan constructor yang menerima parameter type dan message
                return new NotifController(type, message);
            });

            // Load FXML dan bungkus ke Scene
            Scene scene = new Scene(loader.load());

            // Buat stage baru untuk jendela notifikasi
            Stage stage = new Stage();

            // Pasang scene ke stage
            stage.setScene(scene);
            // Hilangkan dekorasi bawaan OS (title bar dan tombol close/minimize/maximize)
            stage.initStyle(StageStyle.UNDECORATED);
            // Set induk/owner dari jendela notifikasi
            stage.initOwner(MAIN_STAGE);
            // Set jendela sebagai modal (biar user tidak bisa klik jendela lain)
            stage.initModality(Modality.WINDOW_MODAL);
            // Posisikan jendela ke tengah layar
            stage.centerOnScreen();
            // Tampilkan jendela notifikasi
            stage.show();
        } catch (Exception e) {
            // Print error ke console
            e.printStackTrace();
        }
    }
}
