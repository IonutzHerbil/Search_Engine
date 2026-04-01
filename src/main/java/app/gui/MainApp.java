package app.gui;

import app.config.IndexConfig;
import app.db.Database;
import app.db.FileRepository;
import app.indexer.IndexerFactory;
import app.processor.ContentExtractor;
import app.search.SearchEngine;
import atlantafx.base.theme.NordDark;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class MainApp extends Application {

  @Override
  public void start(Stage stage) throws Exception {
    Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());

    IndexConfig config = IndexConfig.fromArgs(new String[0]);

    Database db;
    try {
      db = new Database(config.dbPath());
    } catch (SQLException e) {
      showFatalError("Could not open database: " + e.getMessage());
      return;
    }

    FileRepository repository = new FileRepository(db);
    ContentExtractor extractor = new ContentExtractor();
    SearchEngine engine = new SearchEngine(repository);
    IndexerFactory factory = new IndexerFactory(repository, extractor);

    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/gui/search.fxml"));
    Scene scene = new Scene(loader.load());
    scene.getStylesheets().add(getClass().getResource("/app/gui/style.css").toExternalForm());

    SearchController controller = loader.getController();
    controller.init(factory, engine, repository);

    stage.setOnCloseRequest(
        e -> {
          if (controller.isIndexing()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Indexing in progress");
            alert.setHeaderText("Indexing is still running.");
            alert.setContentText("Close anyway? The current indexing run will be lost.");
            alert
                .showAndWait()
                .ifPresent(
                    response -> {
                      if (response != ButtonType.OK) e.consume();
                    });
          }
        });

    stage.setTitle("Search Engine");
    stage.setScene(scene);
    stage.show();
  }

  private void showFatalError(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Fatal Error");
    alert.setHeaderText("Search Engine could not start");
    alert.setContentText(message);
    alert.showAndWait();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
