package app;

import app.config.IndexConfig;
import app.db.Database;
import app.db.FileRepository;
import app.indexer.FileFilter;
import app.indexer.FileIndexer;
import app.processor.ContentExtractor;
import app.search.SearchEngine;

import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        IndexConfig      config     = IndexConfig.fromArgs(args);
        try {
            Database         db         = new Database(config.dbPath());
            FileRepository   repository = new FileRepository(db);
            FileFilter       filter     = new FileFilter(config);
            ContentExtractor extractor  = new ContentExtractor();
            FileIndexer      indexer    = new FileIndexer(config, repository, filter, extractor);
            SearchEngine engine     = new SearchEngine(repository);

            indexer.index();

            var results = engine.search("include", 500);
            System.out.println("\nTest search 'public': " + results.size() + " result(s)");
            results.forEach(r -> System.out.println("  " + r.name() + " | " + r.path()));

            db.close();
        } catch (SQLException e) {
            System.err.println("[FATAL] " + e.getMessage());
        }
    }
}