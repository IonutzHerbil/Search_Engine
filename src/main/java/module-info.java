module app {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.sql;
  requires org.xerial.sqlitejdbc;
  requires atlantafx.base;
  requires org.apache.tika.core;

  opens app.gui to
      javafx.fxml;

  exports app;
  exports app.gui;
}
