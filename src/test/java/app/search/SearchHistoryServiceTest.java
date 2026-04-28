package app.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.db.FileRepository;
import app.model.SearchResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchHistoryServiceTest {

  private FileRepository repository;
  private SearchHistoryService service;

  @BeforeEach
  void setUp() {
    repository = mock(FileRepository.class);
    service = new SearchHistoryService(repository);
  }

  @Test
  void totalSearchesStartsAtZero() {
    assertEquals(0, service.totalSearches());
  }

  @Test
  void totalSearchesIncrementsOnEvent() {
    service.onSearch(event("foo"));
    service.onSearch(event("bar"));
    assertEquals(2, service.totalSearches());
  }

  @Test
  void suggestReturnsMatchingPrefixes() {
    service.onSearch(event("java import"));
    service.onSearch(event("java class"));
    service.onSearch(event("python def"));
    List<String> suggestions = service.suggest("java");
    assertFalse(suggestions.isEmpty());
    assertTrue(suggestions.stream().allMatch(s -> s.startsWith("java")));
  }

  @Test
  void suggestDoesNotReturnExactMatch() {
    service.onSearch(event("java"));
    List<String> suggestions = service.suggest("java");
    assertFalse(suggestions.contains("java"));
  }

  @Test
  void suggestEmptyPrefixReturnsEmpty() {
    service.onSearch(event("java"));
    assertTrue(service.suggest("").isEmpty());
    assertTrue(service.suggest("  ").isEmpty());
  }

  @Test
  void topQueriesOrderedByFrequency() {
    service.onSearch(event("java"));
    service.onSearch(event("java"));
    service.onSearch(event("java"));
    service.onSearch(event("python"));
    service.onSearch(event("python"));
    service.onSearch(event("kotlin"));

    List<Map.Entry<String, Long>> top = service.topQueries(3);
    assertEquals("java", top.get(0).getKey());
    assertEquals("python", top.get(1).getKey());
    assertEquals("kotlin", top.get(2).getKey());
  }

  @Test
  void recentHistoryMostRecentFirst() {
    service.onSearch(event("first"));
    service.onSearch(event("second"));
    service.onSearch(event("third"));

    List<SearchEvent> recent = service.recentHistory(3);
    assertEquals("third", recent.get(0).query());
    assertEquals("second", recent.get(1).query());
    assertEquals("first", recent.get(2).query());
  }

  @Test
  void recentHistoryLimitRespected() {
    for (int i = 0; i < 10; i++) service.onSearch(event("q" + i));
    assertEquals(3, service.recentHistory(3).size());
  }

  @Test
  void boostCallsRepositoryForEachResult() {
    SearchResult r1 = result("/a/Foo.java");
    SearchResult r2 = result("/a/Bar.java");
    service.onResultsReturned(List.of(r1, r2));
    verify(repository).boostPathScore(eq("/a/Foo.java"), anyDouble(), anyDouble());
    verify(repository).boostPathScore(eq("/a/Bar.java"), anyDouble(), anyDouble());
  }

  @Test
  void boostEmptyResultsDoesNotCallRepository() {
    service.onResultsReturned(List.of());
    verify(repository, never()).boostPathScore(any(), anyDouble(), anyDouble());
  }

  @Test
  void boostSortsByBoostedScore() {
    service.onSearch(event("foo"));
    service.onSearch(event("foo"));
    SearchResult frequent = result("/a/foo.java");
    SearchResult rare = result("/a/bar.java");
    List<SearchResult> boosted = service.boost(List.of(frequent, rare));
    assertEquals(2, boosted.size());
  }

  private SearchEvent event(String query) {
    return new SearchEvent(query, Instant.now());
  }

  private SearchResult result(String path) {
    return new SearchResult(
        path, path.substring(path.lastIndexOf('/') + 1), "java", null, -1.0, 0L, 0L, 0.0);
  }
}
