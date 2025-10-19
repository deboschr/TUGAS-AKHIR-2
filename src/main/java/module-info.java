module com.unpar.brokenlinkchecker {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires org.jsoup;
    requires okhttp3;
    requires okio;

    opens com.unpar.brokenlinkchecker to javafx.fxml;

    exports com.unpar.brokenlinkchecker;
}
