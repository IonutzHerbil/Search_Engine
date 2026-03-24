package app.gui;

import app.config.IndexConfig;
import app.db.Database;
import app.db.FileRepository;
import app.indexer.FileFilter;
import app.indexer.FileIndexer;
import app.processor.ContentExtractor;
import app.search.SearchEngine;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.SQLException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        IndexConfig      config     = IndexConfig.fromArgs(getParameters().getRaw().toArray(new String[0]));
        Database         db         = new Database(config.dbPath());
        FileRepository   repository = new FileRepository(db);
        FileFilter       filter     = new FileFilter(config);
        ContentExtractor extractor  = new ContentExtractor();
        FileIndexer      indexer    = new FileIndexer(config, repository, filter, extractor);
        SearchEngine     engine     = new SearchEngine(repository);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/gui/search.fxml"));
        Scene scene = new Scene(loader.load());

        SearchController controller = loader.getController();
        controller.init(indexer, engine);

        stage.setTitle("Search Engine");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}