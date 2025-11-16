package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.controllers.LinkController;
import com.unpar.brokenlinkchecker.controllers.NotificationController;
import com.unpar.brokenlinkchecker.models.Link;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Kelas utama buat ngejalanin aplikasi.
 * 
 * Tugas kelas ini:
 * - Ngejalanin aplikasi JavaFX
 * - Nyimpen referensi ke window utama (mainStage)
 * - Nyediain method buat buka window lain (link detail dan notifikasi)
 * 
 * Intinya kelas ini menjadi pusat kontrol buat semua window di aplikasi
 */
public class Application extends javafx.application.Application {

    // Stage utama aplikasi (buat window utama)
    private static Stage mainStage;

    /**
     * Entry point program.
     */
    public static void main(String[] args) {
        // Method bawaan javafx buat memulai aplikasi
        launch();
    }

    /**
     * Method ini dipanggil otomatis oleh javafx saat aplikasi baru mulai
     */
    @Override
    public void start(Stage stage) {
        // Simpan stage utama supaya bisa dipakai di window lain
        mainStage = stage;

        // Buka window utama
        openMainWindow();
    }

    // ==============================================================
    // ======================== MAIN WINDOW =========================
    // ==============================================================

    /**
     * Method buat ngebuka window utama aplikasi
     */
    public static void openMainWindow() {
        try {
            // Load layout utama dari file FXML
            FXMLLoader loader = new FXMLLoader(
                    Application.class.getResource("/com/unpar/brokenlinkchecker/views/main.fxml"));

            // Buat scene baru untuk window ini
            Scene scene = new Scene(loader.load());

            // Pasang scene ke stage utama
            mainStage.setScene(scene);
            // Hilangin border dan title bar bawaan OS
            mainStage.initStyle(StageStyle.UNDECORATED);
            // Biar window di tengah layar
            mainStage.centerOnScreen();
            // Set ukuran minimum window
            mainStage.setMinWidth(1024);
            mainStage.setMinHeight(600);
            // Tampilin window
            mainStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==============================================================
    // ======================== LINK WINDOW =========================
    // ==============================================================

    /**
     * Method buat ngebuka window link detail
     * 
     * @param link Objek Link yang bakal ditampilin di window ini
     */
    public static void openLinkWindow(Link link) {
        try {
            // Load layout fxml untuk window link detail
            FXMLLoader loader = new FXMLLoader(
                    Application.class.getResource("/com/unpar/brokenlinkchecker/views/link.fxml"));

            // Buat scene baru untuk window ini
            Scene scene = new Scene(loader.load());

            // Ambil controller dari FXML
            LinkController controller = loader.getController();
            // Isi datanya pakai objek Link yang dikirim
            controller.setLink(link);

            // Buat stage baru
            Stage stage = new Stage();
            // Pasang scene ke stage
            stage.setScene(scene);
            // Hilangin border dan title bar bawaan OS
            stage.initStyle(StageStyle.UNDECORATED);
            // Set parent/owner ke stage utama biar window ini nempel ke window utama
            stage.initOwner(mainStage);
            // Pake modal biar window lain ga bisa di-klik
            stage.initModality(Modality.WINDOW_MODAL);
            // Biar window di tengah layar
            stage.centerOnScreen();
            // Tampilin window
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==============================================================
    // ==================== NOTIFICATION WINDOW =====================
    // ==============================================================

    /**
     * Method buat ngebuka window notifikasi
     * 
     * @param type    Jenis notifikasi
     * @param message Pesan yang mau ditampilin
     */
    public static void openNotificationWindow(String type, String message) {
        try {
            // Load layout notifikasi dari FXML
            FXMLLoader loader = new FXMLLoader(
                    Application.class.getResource("/com/unpar/brokenlinkchecker/views/notification.fxml"));

            // Buat scene baru untuk window ini
            Scene scene = new Scene(loader.load());

            // Ambil controller notifikasi
            NotificationController controller = loader.getController();
            // Isi data notifikasi (jenis dan pesan)
            controller.setNotification(type, message);

            // Buat stage baru
            Stage stage = new Stage();
            // Pasang scene ke stage
            stage.setScene(scene);
            // Hilangin border dan title bar bawaan OS
            stage.initStyle(StageStyle.UNDECORATED);
            // Set parent/owner ke stage utama biar window ini nempel ke window utama
            stage.initOwner(mainStage);
            // Pake modal biar window lain ga bisa di-klik
            stage.initModality(Modality.WINDOW_MODAL);
            // Biar window di tengah layar
            stage.centerOnScreen();
            // Tampilin window
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
