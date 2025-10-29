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
 * Kelas utama untuk menjalankan aplikasi Broken Link Checker.
 * Bertanggung jawab untuk menginisialisasi window utama
 * dan menyediakan utilitas pembuka window lain (link & notifikasi).
 */
public class Application extends javafx.application.Application {

    private static Stage primaryStage;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        openMainWindow();
    }

    // ======================== MAIN WINDOW ========================
    public static void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(Application.class.getResource("/com/unpar/brokenlinkchecker/main.fxml"));
            Scene scene = new Scene(loader.load());

            primaryStage.setScene(scene);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(600);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ======================== LINK WINDOW ========================
    public static void openLinkWindow(Link link) {
        try {
            FXMLLoader loader = new FXMLLoader(Application.class.getResource("/com/unpar/brokenlinkchecker/link.fxml"));
            Scene scene = new Scene(loader.load());

            Stage detailStage = new Stage(StageStyle.UNDECORATED);
            detailStage.setScene(scene);
            detailStage.initOwner(primaryStage);
            detailStage.initModality(Modality.WINDOW_MODAL);
            detailStage.centerOnScreen();

            LinkController controller = loader.getController();
            controller.setLink(link);

            detailStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ======================== ALERT WINDOW =======================
    public static void openAlertWindow(String type, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(Application.class.getResource("/com/unpar/brokenlinkchecker/notification.fxml"));
            Scene scene = new Scene(loader.load());

            Stage notifStage = new Stage(StageStyle.UNDECORATED);
            notifStage.setScene(scene);
            notifStage.initOwner(primaryStage);
            notifStage.initModality(Modality.APPLICATION_MODAL);
            notifStage.centerOnScreen();

            NotificationController controller = loader.getController();
            controller.setNotification(type, message);

            notifStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
