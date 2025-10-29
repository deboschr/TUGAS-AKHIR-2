package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.models.Link;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

    // ======================== LINK DETAIL WINDOW =================
    public static void openLinkDetailWindow(Link link) {
        try {
            FXMLLoader loader = new FXMLLoader(Application.class.getResource("/com/unpar/brokenlinkchecker/link_detail.fxml"));
            Scene scene = new Scene(loader.load());

            Stage detailStage = new Stage(StageStyle.UTILITY);
            detailStage.setTitle("Broken Link Detail");
            detailStage.setScene(scene);
            detailStage.initOwner(primaryStage);
            detailStage.initModality(Modality.WINDOW_MODAL);
            detailStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ======================== ALERT WINDOW =======================
    public static void openAlertWindow(String title, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(Application.class.getResource("/com/unpar/brokenlinkchecker/alert.fxml"));
            Scene scene = new Scene(loader.load());

            Stage alertStage = new Stage(StageStyle.UTILITY);
            alertStage.setTitle(title);
            alertStage.setScene(scene);
            alertStage.initOwner(primaryStage);
            alertStage.initModality(Modality.APPLICATION_MODAL);
            alertStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
