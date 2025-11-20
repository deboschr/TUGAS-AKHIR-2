module com.unpar.brokenlinkscanner {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires org.jsoup;
    requires java.net.http;
    requires org.apache.poi.ooxml;

    exports com.unpar.brokenlinkscanner;
    exports com.unpar.brokenlinkscanner.models;
    exports com.unpar.brokenlinkscanner.services;
    exports com.unpar.brokenlinkscanner.controllers;
    exports com.unpar.brokenlinkscanner.utils;

    opens com.unpar.brokenlinkscanner to javafx.fxml;
    opens com.unpar.brokenlinkscanner.services to javafx.fxml;
    opens com.unpar.brokenlinkscanner.controllers to javafx.fxml;
}
