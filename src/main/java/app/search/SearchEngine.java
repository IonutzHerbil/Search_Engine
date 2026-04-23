package app.search;

import app.db.FileRepository;
import app.model.SearchResult;
import java.util.List;

public class SearchEngine {

  private static final int DEFAULT_LIMIT = 200;

  private final FileRepository repository;
  private final SearchRequestParser parser;

  public SearchEngine(FileRepository repository) {
    this.repository = repository;
    this.parser = new SearchRequestParser();
  }

  public List<SearchResult> search(String raw) {
    return search(raw, DEFAULT_LIMIT, 0, SortOrder.RELEVANCE);
  }

  public List<SearchResult> search(String raw, int limit, int offset) {
    return search(raw, limit, offset, SortOrder.RELEVANCE);
  }

  public List<SearchResult> search(String raw, int limit, int offset, SortOrder sort) {
    if (raw == null || raw.isBlank()) return List.of();
    SearchRequest request = parser.parse(raw);
    return repository.search(
        request.terms(), request.extension(), request.directory(), limit, offset, sort);
  }
}
