module com.unpar.brokenlinkchecker {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires org.jsoup;
    requires java.net.http;

    exports com.unpar.brokenlinkchecker;
    exports com.unpar.brokenlinkchecker.cores;
    exports com.unpar.brokenlinkchecker.models;
    exports com.unpar.brokenlinkchecker.controllers;
    exports com.unpar.brokenlinkchecker.utils;

    opens com.unpar.brokenlinkchecker to javafx.fxml;
    opens com.unpar.brokenlinkchecker.cores to javafx.fxml;
    opens com.unpar.brokenlinkchecker.controllers to javafx.fxml;
    
}
