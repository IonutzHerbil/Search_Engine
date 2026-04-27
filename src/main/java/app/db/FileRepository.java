package app.db;

import app.model.FileRecord;
import app.model.SearchResult;
import app.search.RankingStrategy;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileRepository {

  private final Connection connection;

  public FileRepository(Database database) {
    this.connection = database.getConnection();
  }

  public List<SearchResult> search(
      String query,
      List<String> extensions,
      List<String> directories,
      int limit,
      int offset,
      RankingStrategy strategy) {

    if (query == null || query.isBlank()) {
      return metadataSearch(extensions, directories, limit, offset, strategy);
    }

    String inner =
        strategy.requiresBm25()
            ? "SELECT path, bm25(files_fts) AS r FROM files_fts WHERE files_fts MATCH ? LIMIT 5000"
            : "SELECT path, 0.0              AS r FROM files_fts WHERE files_fts MATCH ? LIMIT 5000";

    StringBuilder sql =
        new StringBuilder(
            "SELECT f.path, f.name, f.extension, f.lastModified, f.preview, f.sizeBytes, f.pathScore, fts.r "
                + "FROM ("
                + inner
                + ") fts "
                + "JOIN files f ON f.path = fts.path ");

    if (!extensions.isEmpty()) {
      sql.append("AND LOWER(f.extension) IN (")
          .append("?,".repeat(extensions.size()).replaceAll(",$", ""))
          .append(") ");
    }
    for (int d = 0; d < directories.size(); d++) sql.append("AND f.path LIKE ? ");

    sql.append(strategy.orderByClause());
    sql.append("LIMIT ? OFFSET ?");

    List<SearchResult> results = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
      int i = 1;
      stmt.setString(i++, query);
      for (String ext : extensions) stmt.setString(i++, ext);
      for (String dir : directories) stmt.setString(i++, "%" + dir + "%");
      stmt.setInt(i++, limit);
      stmt.setInt(i, offset);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          results.add(
              new SearchResult(
                  rs.getString("path"),
                  rs.getString("name"),
                  rs.getString("extension"),
                  rs.getString("preview"),
                  rs.getDouble("r"),
                  rs.getLong("lastModified"),
                  rs.getLong("sizeBytes"),
                  rs.getDouble("pathScore")));
        }
      }
    } catch (SQLException e) {
      System.err.println("[SEARCH ERROR] " + e.getMessage());
    }
    return results;
  }

  private List<SearchResult> metadataSearch(
      List<String> extensions,
      List<String> directories,
      int limit,
      int offset,
      RankingStrategy strategy) {

    StringBuilder sql =
        new StringBuilder(
            "SELECT path, name, extension, lastModified, preview, sizeBytes, pathScore, 0.0 AS r "
                + "FROM files WHERE 1=1 ");

    if (!extensions.isEmpty()) {
      sql.append("AND LOWER(extension) IN (")
          .append("?,".repeat(extensions.size()).replaceAll(",$", ""))
          .append(") ");
    }
    for (int d = 0; d < directories.size(); d++) sql.append("AND path LIKE ? ");

    String orderBy =
        strategy
            .orderByClause()
            .replace("f.lastModified", "lastModified")
            .replace("f.sizeBytes", "sizeBytes")
            .replace("f.pathScore", "pathScore")
            .replace("f.name", "name")
            .replace("fts.r", "0");

    sql.append(orderBy);
    sql.append("LIMIT ? OFFSET ?");

    List<SearchResult> results = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
      int i = 1;
      for (String ext : extensions) stmt.setString(i++, ext);
      for (String dir : directories) stmt.setString(i++, "%" + dir + "%");
      stmt.setInt(i++, limit);
      stmt.setInt(i, offset);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          results.add(
              new SearchResult(
                  rs.getString("path"),
                  rs.getString("name"),
                  rs.getString("extension"),
                  rs.getString("preview"),
                  rs.getDouble("r"),
                  rs.getLong("lastModified"),
                  rs.getLong("sizeBytes"),
                  rs.getDouble("pathScore")));
        }
      }
    } catch (SQLException e) {
      System.err.println("[METADATA SEARCH ERROR] " + e.getMessage());
    }
    return results;
  }

  public List<SearchResult> search(
      String query, List<String> extensions, List<String> directories, int limit, int offset) {
    return search(query, extensions, directories, limit, offset, RankingStrategy.RELEVANCE);
  }

  public List<SearchResult> search(
      String query, List<String> extensions, List<String> directories, int limit) {
    return search(query, extensions, directories, limit, 0, RankingStrategy.RELEVANCE);
  }

  public void deleteStale(String rootPath) {
    for (String path : getPathsUnder(rootPath)) {
      if (!Path.of(path).toFile().exists()) delete(path);
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
    try (PreparedStatement d1 = connection.prepareStatement("DELETE FROM files WHERE path = ?");
        PreparedStatement d2 =
            connection.prepareStatement("DELETE FROM files_fts WHERE path = ?")) {
      d1.setString(1, path);
      d1.executeUpdate();
      d2.setString(1, path);
      d2.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      try {
        connection.rollback();
      } catch (SQLException ignored) {
      }
      System.err.println("[DELETE ERROR] " + e.getMessage());
    }
  }

  public List<String> getDistinctExtensions() {
    List<String> extensions = new ArrayList<>();
    try (PreparedStatement stmt =
        connection.prepareStatement(
            "SELECT DISTINCT extension FROM files "
                + "WHERE extension IS NOT NULL AND extension != '' ORDER BY extension")) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) extensions.add(rs.getString(1));
      }
    } catch (SQLException e) {
      System.err.println("[QUERY ERROR] " + e.getMessage());
    }
    return extensions;
  }

  public java.util.Map<String, Long> getLastModifiedMap(String rootPath) {
    java.util.Map<String, Long> map = new java.util.HashMap<>();
    try (PreparedStatement stmt =
        connection.prepareStatement("SELECT path, lastModified FROM files WHERE path LIKE ?")) {
      stmt.setString(1, rootPath + "%");
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) map.put(rs.getString(1), rs.getLong(2));
      }
    } catch (SQLException e) {
      System.err.println("[PRELOAD ERROR] " + e.getMessage());
    }
    return map;
  }

  public void upsertNoCommit(FileRecord record) {
    try (PreparedStatement d1 = connection.prepareStatement("DELETE FROM files WHERE path = ?");
        PreparedStatement d2 = connection.prepareStatement("DELETE FROM files_fts WHERE path = ?");
        PreparedStatement i1 =
            connection.prepareStatement(
                "INSERT INTO files (path, name, extension, sizeBytes, lastModified, preview, pathScore) VALUES (?,?,?,?,?,?,?)");
        PreparedStatement i2 =
            connection.prepareStatement(
                "INSERT INTO files_fts (path, name, content) VALUES (?,?,?)")) {

      d1.setString(1, record.path());
      d1.executeUpdate();
      d2.setString(1, record.path());
      d2.executeUpdate();

      i1.setString(1, record.path());
      i1.setString(2, record.name());
      i1.setString(3, record.extension());
      i1.setLong(4, record.sizeBytes());
      i1.setLong(5, record.lastModified());
      i1.setString(6, record.preview());
      i1.setDouble(7, record.pathScore());
      i1.executeUpdate();

      i2.setString(1, record.path());
      i2.setString(2, record.name());
      i2.setString(3, record.content() != null ? record.content() : "");
      i2.executeUpdate();
    } catch (SQLException e) {
      System.err.println("[DB ERROR] Failed to save " + record.name() + ": " + e.getMessage());
    }
  }

  public void commit() {
    try {
      connection.commit();
    } catch (SQLException e) {
      try {
        connection.rollback();
      } catch (SQLException ignored) {
      }
      System.err.println("[COMMIT ERROR] " + e.getMessage());
    }
  }

  public void boostPathScore(String path, double delta, double max) {
    try (PreparedStatement stmt =
        connection.prepareStatement(
            "UPDATE files SET pathScore = MIN(pathScore + ?, ?) WHERE path = ?")) {
      stmt.setDouble(1, delta);
      stmt.setDouble(2, max);
      stmt.setString(3, path);
      stmt.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      System.err.println("[BOOST ERROR] " + e.getMessage());
    }
  }

  public void deletePath(String path) {
    try (PreparedStatement d1 = connection.prepareStatement("DELETE FROM files WHERE path = ?");
        PreparedStatement d2 =
            connection.prepareStatement("DELETE FROM files_fts WHERE path = ?")) {
      d1.setString(1, path);
      d1.executeUpdate();
      d2.setString(1, path);
      d2.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      System.err.println("[DELETE ERROR] " + e.getMessage());
    }
  }

  public long countFiles() {
    try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM files");
        ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) return rs.getLong(1);
    } catch (SQLException e) {
      System.err.println("[STATS ERROR] " + e.getMessage());
    }
    return 0;
  }

  public List<Map.Entry<String, Long>> topExtensions(int n) {
    List<Map.Entry<String, Long>> result = new ArrayList<>();
    try (PreparedStatement stmt =
        connection.prepareStatement(
            "SELECT LOWER(extension) AS ext, COUNT(*) AS cnt FROM files "
                + "WHERE extension IS NOT NULL AND extension != '' "
                + "GROUP BY LOWER(extension) ORDER BY cnt DESC LIMIT ?")) {
      stmt.setInt(1, n);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) result.add(Map.entry(rs.getString("ext"), rs.getLong("cnt")));
      }
    } catch (SQLException e) {
      System.err.println("[STATS ERROR] " + e.getMessage());
    }
    return result;
  }
}
