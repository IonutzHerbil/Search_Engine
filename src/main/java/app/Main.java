package app;

import app.config.IndexConfig;
import app.db.Database;
import app.db.FileRepository;
import app.indexer.FileFilter;
import app.indexer.FileIndexer;
import app.processor.ContentExtractor;

import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        IndexConfig    config     = IndexConfig.fromArgs(args);
        try {
            Database       db         = new Database(config.dbPath());
            FileRepository repository = new FileRepository(db);
            FileFilter     filter     = new FileFilter(config);
            ContentExtractor extractor = new ContentExtractor();
            FileIndexer    indexer    = new FileIndexer(config, repository, filter, extractor);

            indexer.index();

            db.close();
        } catch (SQLException e) {
            System.err.println("[FATAL] " + e.getMessage());
        }
    }
}