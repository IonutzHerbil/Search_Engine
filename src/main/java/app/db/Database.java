package app.db;

import java.sql.*;

public class Database {

  private final Connection connection;

  public Database(String dbPath) throws SQLException {
    connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
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
