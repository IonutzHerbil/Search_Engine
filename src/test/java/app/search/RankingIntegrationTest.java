package app.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.db.Database;
import app.db.FileRepository;
import app.model.FileRecord;
import app.model.SearchResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RankingIntegrationTest {

  private Database db;
  private FileRepository repo;
  private SearchEngine engine;
  private SearchHistoryService history;

  @BeforeEach
  void setUp() throws Exception {
    db = new Database(":memory:");
    repo = new FileRepository(db);
    engine = new SearchEngine(repo);
    history = new SearchHistoryService(repo);
    engine.addObserver(history);
  }

  private void insertMockFile(
      String path, String name, String content, long size, double pathScore) {
    FileRecord record =
        new FileRecord(
            path, name, "txt", size, System.currentTimeMillis(), "preview", content, pathScore);
    repo.upsertNoCommit(record);
    repo.commit();
  }

  @Test
  void testTitleVersusContentBm25() {
    insertMockFile("/a/LoginController.java", "LoginController.java", "empty text", 100, 1.0);
    insertMockFile("/b/Utils.java", "Utils.java", "handles the login process", 100, 1.0);

    List<SearchResult> results = engine.search("login");

    assertEquals(2, results.size());
    assertEquals("LoginController.java", results.get(0).name());
    assertEquals("Utils.java", results.get(1).name());
  }

  @Test
  void testTermDensity() {
    insertMockFile("/a/A.txt", "A.txt", "database connection pool", 100, 1.0);
    insertMockFile("/b/B.txt", "B.txt", "database", 100, 1.0);
    insertMockFile("/c/C.txt", "C.txt", "connection connection connection", 100, 1.0);

    List<SearchResult> results = engine.search("database connection");

    assertTrue(results.stream().anyMatch(r -> r.name().equals("A.txt")));
    assertEquals("A.txt", results.get(0).name());
  }

  @Test
  void testPathScoreTieBreaker() {
    insertMockFile("/src/main/A.txt", "A.txt", "target data", 100, 5.0); // Higher pathScore
    insertMockFile("/src/test/A.txt", "A.txt", "target data", 100, 1.0); // Lower pathScore

    List<SearchResult> results = engine.search("target");
    assertEquals("/src/main/A.txt", results.get(0).path());
  }

  @Test
  void testHistoryBehaviorBoosting() {
    insertMockFile("/a/AppConfig.java", "AppConfig.java", "config file", 100, 1.0);
    insertMockFile("/b/IndexConfig.java", "IndexConfig.java", "config file", 100, 1.0);

    List<SearchResult> initialResults = engine.search("indexconfig");

    for (int i = 0; i < 50; i++) {
      history.onSearch(new SearchEvent("config", java.time.Instant.now()));
    }

    List<SearchResult> rawResults = engine.search("config");
    List<SearchResult> boostedResults = history.boost(rawResults);
    assertEquals("IndexConfig.java", boostedResults.get(0).name());
  }

  @Test
  void testStrategySwapping() {
    insertMockFile("/a.txt", "a.txt", "data", 1000, 1.0);
    insertMockFile("/b.txt", "b.txt", "data", 5000, 1.0);
    insertMockFile("/c.txt", "c.txt", "data", 200, 1.0);

    List<SearchResult> results = engine.search("", 10, 0, RankingStrategy.SIZE);

    assertEquals(3, results.size());
    assertEquals(5000, results.get(0).sizeBytes());
    assertEquals(1000, results.get(1).sizeBytes());
    assertEquals(200, results.get(2).sizeBytes());
  }
}
