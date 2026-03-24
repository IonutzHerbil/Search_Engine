module app {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires atlantafx.base;
    opens app.gui to javafx.fxml;
    exports app;
    exports app.gui;
}