package app.search;

import app.db.FileRepository;
import app.model.SearchResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SearchEngine {

  private static final int DEFAULT_LIMIT = 200;

  private final FileRepository repository;
  private final SearchRequestParser parser;
  private final List<SearchObserver> observers = new ArrayList<>();

  public SearchEngine(FileRepository repository) {
    this.repository = repository;
    this.parser = new SearchRequestParser();
  }

  public void addObserver(SearchObserver observer) {
    observers.add(observer);
  }

  public List<SearchResult> search(String raw) {
    return search(raw, DEFAULT_LIMIT, 0, RankingStrategy.RELEVANCE);
  }

  public List<SearchResult> search(String raw, int limit, int offset) {
    return search(raw, limit, offset, RankingStrategy.RELEVANCE);
  }

  public List<SearchResult> search(String raw, int limit, int offset, RankingStrategy strategy) {
    if (raw == null || raw.isBlank()) return List.of();
    SearchRequest request = parser.parse(raw);
    notifyObservers(raw);
    return repository.search(
        request.terms(), request.extensions(), request.directories(), limit, offset, strategy);
  }

  private void notifyObservers(String query) {
    SearchEvent event = new SearchEvent(query, Instant.now());
    observers.forEach(o -> o.onSearch(event));
  }
}
