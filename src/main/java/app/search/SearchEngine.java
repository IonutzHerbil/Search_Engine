package app.search;

import app.db.FileRepository;
import app.model.SearchResult;
import app.search.preprocessor.LogicDecorator;
import app.search.preprocessor.QueryPreProcessor;
import app.search.preprocessor.SanitizationDecorator;
import app.search.preprocessor.SynonymDecorator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SearchEngine {

  private static final int DEFAULT_LIMIT = 200;

  private final FileRepository repository;
  private final SearchRequestParser parser;
  private final QueryPreProcessor pipeline;
  private final List<SearchObserver> observers = new ArrayList<>();

  public SearchEngine(FileRepository repository) {
    this.repository = repository;
    this.parser = new SearchRequestParser();
    this.pipeline =
        new LogicDecorator(
            new SynonymDecorator(new SanitizationDecorator(raw -> raw == null ? "" : raw.trim())));
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
    String safeRaw = raw == null ? "" : raw;
    String processed = pipeline.process(safeRaw);
    SearchRequest request = parser.parse(processed);
    notifyObservers(safeRaw);
    return repository.search(
        request.terms(),
        request.extensions(),
        request.directories(),
        limit,
        offset,
        strategy,
        request.colorFilter());
  }

  private void notifyObservers(String query) {
    SearchEvent event = new SearchEvent(query, Instant.now());
    observers.forEach(o -> o.onSearch(event));
  }
}
