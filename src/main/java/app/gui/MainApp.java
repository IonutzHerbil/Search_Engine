package app.gui;

import app.config.IndexConfig;
import app.db.Database;
import app.db.FileRepository;
import app.search.SearchEngine;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import atlantafx.base.theme.NordDark;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        IndexConfig    config     = IndexConfig.fromArgs(new String[0]);
        Database       db         = new Database(config.dbPath());
        FileRepository repository = new FileRepository(db);
        SearchEngine   engine     = new SearchEngine(repository);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/gui/search.fxml"));
        Scene scene = new Scene(loader.load());

        SearchController controller = loader.getController();
        controller.init(repository, engine);

        stage.setTitle("Search Engine");
        stage.setScene(scene);
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}