package app.search;

import app.db.FileRepository;
import app.model.SearchResult;

import java.util.List;

public class SearchEngine {

    private static final int DEFAULT_LIMIT = 10;

    private final FileRepository      repository;
    private final SearchRequestParser parser;

    public SearchEngine(FileRepository repository) {
        this.repository = repository;
        this.parser     = new SearchRequestParser();
    }

    public List<SearchResult> search(String raw) {
        return search(raw, DEFAULT_LIMIT);
    }

    public List<SearchResult> search(String raw, int limit) {
        if (raw == null || raw.isBlank()) return List.of();
        SearchRequest request = parser.parse(raw);
        if (request.terms().isBlank()) return List.of();
        return repository.search(request.terms(), request.extension(), limit);
    }
}