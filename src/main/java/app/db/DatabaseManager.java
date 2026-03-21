package app.db;

import app.model.FileRecord;

import java.sql.*;

public class DatabaseManager {

    private final Connection connection;

    public DatabaseManager(String dbPath) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        connection.setAutoCommit(false);
        initSchema();
        connection.commit();
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS files (
                    path         TEXT PRIMARY KEY,
                    name         TEXT NOT NULL,
                    extension    TEXT,
                    sizeBytes    INTEGER NOT NULL,
                    lastModified INTEGER NOT NULL,
                    preview      TEXT
                )
            """);

            stmt.execute("""
                CREATE VIRTUAL TABLE IF NOT EXISTS files_fts USING fts5(
                    path UNINDEXED,
                    name,
                    content
                )
            """);
        }
    }

    public long getLastModified(String path) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT lastModified FROM files WHERE path = ?")) {
            stmt.setString(1, path);
            try(ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException ignored) {}
        return -1;
    }

    public void upsert(FileRecord record) {
            try (PreparedStatement del1 = connection.prepareStatement("DELETE FROM files WHERE path = ?");
                 PreparedStatement del2 = connection.prepareStatement("DELETE FROM files_fts WHERE path = ?");

                 PreparedStatement ins1 = connection.prepareStatement("INSERT INTO files (path, name, extension, sizeBytes, lastModified, preview) VALUES (?,?,?,?,?,?)");
                 PreparedStatement ins2 = connection.prepareStatement("INSERT INTO files_fts (path, name, content) VALUES (?,?,?)")) {

                del1.setString(1, record.path());
                del1.executeUpdate();
                del2.setString(1, record.path());
                del2.executeUpdate();

                ins1.setString(1, record.path());
                ins1.setString(2, record.name());
                ins1.setString(3, record.extension());
                ins1.setLong(4, record.sizeBytes());
                ins1.setLong(5, record.lastModified());
                ins1.setString(6, record.preview());
                ins1.executeUpdate();

                ins2.setString(1, record.path());
                ins2.setString(2, record.name());
                ins2.setString(3, record.content() != null ? record.content() : "");
                ins2.executeUpdate();

                connection.commit();
            }catch (SQLException e) {
            try { connection.rollback(); }
                catch (SQLException ignored) {}
                System.err.println("[DB ERROR] Failed to save " + record.name() + ": " + e.getMessage());
            }
    }

    public void close() {
        try {
            connection.close();
        }
        catch (SQLException e) {
            System.err.println("[WARN] Could not close DB: " + e.getMessage());
        }
    }
}