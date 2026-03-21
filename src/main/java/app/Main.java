package app;

import app.config.IndexConfig;
import app.db.DatabaseManager;
import app.indexer.FileIndexer;

import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        IndexConfig config = IndexConfig.fromArgs(args);
        try {
            DatabaseManager db = new DatabaseManager(config.dbPath());
            new FileIndexer(config, db).index();
            db.close();
        } catch (SQLException e) {
            System.err.println("[FATAL] Cannot open database: " + e.getMessage());
        }
    }
}