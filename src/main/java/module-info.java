module com.unpar.brokenlinkchecker {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.unpar.brokenlinkchecker to javafx.fxml;
    exports com.unpar.brokenlinkchecker;
}