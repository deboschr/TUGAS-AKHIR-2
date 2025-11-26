package com.unpar.brokenlinkscanner;

import com.unpar.brokenlinkscanner.controllers.LinkController;
import com.unpar.brokenlinkscanner.controllers.NotificationController;
import com.unpar.brokenlinkscanner.models.Link;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

public class Application extends javafx.application.Application {
    private static Stage MAIN_STAGE;
    private static final URL MAIN_FXML = Application.class
            .getResource("/com/unpar/brokenlinkscanner/scenes/main-scene.fxml");
    private static final URL LINK_FXML = Application.class
            .getResource("/com/unpar/brokenlinkscanner/scenes/link-scene.fxml");
    private static final URL NOTIFICATION_FXML = Application.class
            .getResource("/com/unpar/brokenlinkscanner/scenes/notification-scene.fxml");

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        MAIN_STAGE = stage;

        openMainWindow();
    }

    public static void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(MAIN_FXML);

            Scene scene = new Scene(loader.load());

            MAIN_STAGE.setScene(scene);
            MAIN_STAGE.initStyle(StageStyle.UNDECORATED);
            MAIN_STAGE.centerOnScreen();
            MAIN_STAGE.setMaximized(true);

            MAIN_STAGE.setMinWidth(1024);
            MAIN_STAGE.setMinHeight(600);

            MAIN_STAGE.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void openLinkWindow(Link link) {
        try {
            FXMLLoader loader = new FXMLLoader(LINK_FXML);

            Scene scene = new Scene(loader.load());

            LinkController controller = loader.getController();
            controller.setLink(link);

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initOwner(MAIN_STAGE);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void openNotificationWindow(String type, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(NOTIFICATION_FXML);

            Scene scene = new Scene(loader.load());

            NotificationController controller = loader.getController();
            controller.setNotification(type, message);

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initOwner(MAIN_STAGE);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
