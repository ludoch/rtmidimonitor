module com.rtmidimonitor {
    requires javafx.controls;
    requires javafx.fxml;
    requires rtmidijava;
    requires java.prefs;

    opens com.rtmidimonitor to javafx.fxml;
    exports com.rtmidimonitor;
}
