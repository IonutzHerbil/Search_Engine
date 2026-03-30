package app.gui;

import app.config.IndexConfig;
import app.db.Database;
import app.db.FileRepository;
import app.indexer.IndexerFactory;
import app.processor.ContentExtractor;
import app.search.SearchEngine;
import atlantafx.base.theme.NordDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

  @Override
  public void start(Stage stage) throws Exception {
    Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());

    IndexConfig config = IndexConfig.fromArgs(new String[0]);
    Database db = new Database(config.dbPath());
    FileRepository repository = new FileRepository(db);
    ContentExtractor extractor = new ContentExtractor();
    SearchEngine engine = new SearchEngine(repository);
    IndexerFactory factory = new IndexerFactory(repository, extractor);

    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/gui/search.fxml"));
    Scene scene = new Scene(loader.load());
    scene.getStylesheets().add(getClass().getResource("/app/gui/style.css").toExternalForm());

    SearchController controller = loader.getController();
    controller.init(factory, engine, repository);

    stage.setTitle("Search Engine");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
