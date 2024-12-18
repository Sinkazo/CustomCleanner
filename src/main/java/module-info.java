module org.example.customcleanner {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens org.example.customcleanner to javafx.fxml;
    exports org.example.customcleanner;
}