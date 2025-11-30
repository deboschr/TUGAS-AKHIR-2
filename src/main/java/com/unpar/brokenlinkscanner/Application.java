package com.unpar.brokenlinkscanner;

import com.unpar.brokenlinkscanner.controllers.LinkController;
import com.unpar.brokenlinkscanner.controllers.NotificationController;
import com.unpar.brokenlinkscanner.models.Link;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;

public class Application extends javafx.application.Application {
    private static Stage MAIN_STAGE;

    @Override
    public void start(Stage stage) {
        MAIN_STAGE = stage;

        openMainWindow();
    }

    public static void main(String[] args) {
        launch();
    }

    public static void openMainWindow() {
        try {
            URL fxml = Application.class.getResource("/com/unpar/brokenlinkscanner/scenes/main-scene.fxml");

            FXMLLoader loader = new FXMLLoader(fxml);

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
            URL fxml = Application.class.getResource("/com/unpar/brokenlinkscanner/scenes/link-scene.fxml");

            FXMLLoader loader = new FXMLLoader(fxml);

            loader.setControllerFactory(param -> {
                if (param == LinkController.class) {
                    return new LinkController(link);  // inject Link
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });

            Scene scene = new Scene(loader.load());
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
            URL fxml = Application.class.getResource("/com/unpar/brokenlinkscanner/scenes/notification-scene.fxml");

            Stage stage = getStage(type, message, fxml);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initOwner(MAIN_STAGE);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Stage getStage(String type, String message, URL fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(fxml);

        loader.setControllerFactory(param -> {
            if (param == NotificationController.class) {
                return new NotificationController(type, message); // inject type & message
            }
            try {
                return param.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();

        stage.setScene(scene);
        return stage;
    }
}
