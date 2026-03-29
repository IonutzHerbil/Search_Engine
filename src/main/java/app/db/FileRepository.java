package app.db;

import app.model.FileRecord;
import app.model.SearchResult;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileRepository {

  private final Connection connection;

  public FileRepository(Database database) {
    this.connection = database.getConnection();
  }

  public long getLastModified(String path) {
    try (PreparedStatement stmt =
        connection.prepareStatement("SELECT lastModified FROM files WHERE path = ?")) {
      stmt.setString(1, path);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) return rs.getLong(1);
      }
    } catch (SQLException ignored) {
    }
    return -1;
  }

  public void upsert(FileRecord record) {
    try (PreparedStatement del1 = connection.prepareStatement("DELETE FROM files WHERE path = ?");
        PreparedStatement del2 =
            connection.prepareStatement("DELETE FROM files_fts WHERE path = ?");
        PreparedStatement ins1 =
            connection.prepareStatement(
                "INSERT INTO files (path, name, extension, sizeBytes, lastModified, preview) VALUES (?,?,?,?,?,?)");
        PreparedStatement ins2 =
            connection.prepareStatement(
                "INSERT INTO files_fts (path, name, content) VALUES (?,?,?)")) {

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
    } catch (SQLException e) {
      try {
        connection.rollback();
      } catch (SQLException ignored) {
      }
      System.err.println("[DB ERROR] Failed to save " + record.name() + ": " + e.getMessage());
    }
  }

  public List<SearchResult> search(String query, String extension, String directory, int limit) {
    StringBuilder sql =
        new StringBuilder(
            """
        SELECT f.path, f.name, f.extension, f.lastModified, f.preview, rank
        FROM files_fts fts
        JOIN files f ON f.path = fts.path
        WHERE files_fts MATCH ?
        """);

    if (extension != null) sql.append("AND f.extension = ? ");
    if (directory != null) sql.append("AND f.path LIKE ? ");
    sql.append("ORDER BY rank LIMIT ?");

    List<SearchResult> results = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
      int i = 1;
      stmt.setString(i++, query);
      if (extension != null) stmt.setString(i++, extension);
      if (directory != null) stmt.setString(i++, directory + "%");
      stmt.setInt(i, limit);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          results.add(
              new SearchResult(
                  rs.getString("path"),
                  rs.getString("name"),
                  rs.getString("extension"),
                  rs.getString("preview"),
                  rs.getDouble("rank"),
                  rs.getLong("lastModified")));
        }
      }
    } catch (SQLException e) {
      System.err.println("[SEARCH ERROR] " + e.getMessage());
    }
    return results;
  }

  public void deleteStale(String rootPath) {
    List<String> paths = getPathsUnder(rootPath);
    for (String path : paths) {
      if (!Path.of(path).toFile().exists()) {
        delete(path);
      }
    }
  }

  private List<String> getPathsUnder(String rootPath) {
    List<String> paths = new ArrayList<>();
    try (PreparedStatement stmt =
        connection.prepareStatement("SELECT path FROM files WHERE path LIKE ?")) {
      stmt.setString(1, rootPath + "%");
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) paths.add(rs.getString("path"));
      }
    } catch (SQLException e) {
      System.err.println("[CLEANUP ERROR] " + e.getMessage());
    }
    return paths;
  }

  private void delete(String path) {
    try (PreparedStatement del1 = connection.prepareStatement("DELETE FROM files WHERE path = ?");
        PreparedStatement del2 =
            connection.prepareStatement("DELETE FROM files_fts WHERE path = ?")) {
      del1.setString(1, path);
      del1.executeUpdate();
      del2.setString(1, path);
      del2.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      try {
        connection.rollback();
      } catch (SQLException ignored) {
      }
      System.err.println("[DELETE ERROR] " + e.getMessage());
    }
  }
}
