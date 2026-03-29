package app;

import app.cli.CLI;
import app.config.IndexConfig;
import app.db.Database;
import app.db.FileRepository;
import app.indexer.IndexerFactory;
import app.processor.ContentExtractor;
import app.search.SearchEngine;
import java.sql.SQLException;

public class Main {

  public static void main(String[] args) {
    IndexConfig config = IndexConfig.fromArgs(args);
    try {
      Database db = new Database(config.dbPath());
      FileRepository repository = new FileRepository(db);
      ContentExtractor extractor = new ContentExtractor();
      IndexerFactory factory = new IndexerFactory(repository, extractor);
      SearchEngine engine = new SearchEngine(repository);

      new CLI(factory, engine).run();

      db.close();
    } catch (SQLException e) {
      System.err.println("[FATAL] " + e.getMessage());
    }
  }
}
