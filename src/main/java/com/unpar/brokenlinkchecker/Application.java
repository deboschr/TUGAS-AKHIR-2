package com.unpar.brokenlinkchecker;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Application extends javafx.application.Application {

    /**
     * Method main() jadi titik masuk program.
     */
    public static void main(String[] args) {
        launch(); // launch() dipanggil untuk memulai lifecycle JavaFX.
    }

    /**
     * Method start() otomatis dijalankan setelah method launch() dijalankan.
     * Tugasnya: menyiapkan GUI utama aplikasi.
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Loader buat baca file FXML (layout utama aplikasi)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/unpar/brokenlinkchecker/main.fxml"));
        Parent root = loader.load();

        // Bungkus root node ke dalam Scene
        Scene scene = new Scene(root);

        // Kasih scene ke stage
        stage.setScene(scene);

        // Hilangkan title bar default
        stage.initStyle(StageStyle.UNDECORATED);

        // Konfigurasi ukuran (px)
        stage.setMinWidth(1024);
        stage.setMinHeight(600);

        // Maximized window
        stage.setMaximized(true);

        // Biar muncul di tengah layar
        stage.centerOnScreen();

        // tampilkan window
        stage.show();
    }

}
