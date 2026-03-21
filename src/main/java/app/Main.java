package app;

import app.config.IndexConfig;
import app.db.DatabaseManager;
import app.indexer.FileFilter;
import app.indexer.FileIndexer;
import app.processor.ContentExtractor;

import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        IndexConfig config = IndexConfig.fromArgs(args);
        try {
            DatabaseManager db = new DatabaseManager(config.dbPath());
            FileFilter filter = new FileFilter(config);
            ContentExtractor extractor = new ContentExtractor();
            FileIndexer indexer = new FileIndexer(config, db, filter, extractor);
            new FileIndexer(config, db, filter,extractor).index();
            db.close();
        } catch (SQLException e) {
            System.err.println("[FATAL] Cannot open database: " + e.getMessage());
        }
    }
}