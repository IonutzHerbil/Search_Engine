package app.db;

import java.sql.*;

public class Database {

  private final Connection connection;

  public Database(String dbPath) throws SQLException {
    connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("PRAGMA journal_mode=WAL;");
      stmt.execute("PRAGMA cache_size = -32000;");
      stmt.execute("PRAGMA synchronous = NORMAL;");
    }
    connection.setAutoCommit(false);
    initSchema();
    connection.commit();
  }

  private void initSchema() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(
          """
          CREATE TABLE IF NOT EXISTS files (
              path         TEXT PRIMARY KEY,
              name         TEXT NOT NULL,
              extension    TEXT,
              sizeBytes    INTEGER NOT NULL,
              lastModified INTEGER NOT NULL,
              preview      TEXT
          )
      """);
      stmt.execute(
          """
          CREATE VIRTUAL TABLE IF NOT EXISTS files_fts USING fts5(
              path UNINDEXED,
              name,
              content
          )
      """);
      stmt.execute("CREATE INDEX IF NOT EXISTS idx_files_extension    ON files(extension)");
      stmt.execute("CREATE INDEX IF NOT EXISTS idx_files_lastModified ON files(lastModified DESC)");
      stmt.execute("CREATE INDEX IF NOT EXISTS idx_files_sizeBytes    ON files(sizeBytes DESC)");
    }
  }

  public Connection getConnection() {
    return connection;
  }

  public void close() {
    try {
      connection.close();
    } catch (SQLException e) {
      System.err.println("[WARN] Could not close DB: " + e.getMessage());
    }
  }
}
