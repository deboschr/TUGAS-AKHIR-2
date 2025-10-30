module com.unpar.brokenlinkchecker {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires org.jsoup;
    requires okhttp3;
    requires okio;
    requires org.apache.poi.ooxml;
    requires com.google.gson;

    exports com.unpar.brokenlinkchecker;
    exports com.unpar.brokenlinkchecker.models;
    exports com.unpar.brokenlinkchecker.cores;
    exports com.unpar.brokenlinkchecker.controllers;

    opens com.unpar.brokenlinkchecker to javafx.fxml;
    opens com.unpar.brokenlinkchecker.cores to javafx.fxml;
    opens com.unpar.brokenlinkchecker.controllers to javafx.fxml;
    opens com.unpar.brokenlinkchecker.models to com.google.gson;
}
