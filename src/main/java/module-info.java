module org.example.customcleanner {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens org.example.customcleanner to javafx.fxml;
    exports org.example.customcleanner;
    exports org.example.customcleanner.model;
    opens org.example.customcleanner.model to javafx.fxml;
    exports org.example.customcleanner.util;
    opens org.example.customcleanner.util to javafx.fxml;
    exports org.example.customcleanner.service;
    opens org.example.customcleanner.service to javafx.fxml;
}