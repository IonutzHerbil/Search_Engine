package app.search;

import app.db.FileRepository;
import app.model.SearchResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SearchHistoryService implements SearchObserver {

  private static final int MAX_HISTORY = 500;
  private static final int MAX_SUGGESTION = 5;
  private static final double BOOST_PER_HIT = 0.01;
  private static final double MAX_BOOST = 2.0;

  private final List<SearchEvent> history = new CopyOnWriteArrayList<>();
  private final Map<String, Long> frequency = new ConcurrentHashMap<>();
  private final FileRepository repository;

  public SearchHistoryService(FileRepository repository) {
    this.repository = repository;
  }

  @Override
  public void onSearch(SearchEvent event) {
    if (history.size() >= MAX_HISTORY) history.remove(0);
    history.add(event);
    frequency.merge(normalise(event.query()), 1L, Long::sum);
  }

  public void onResultsReturned(List<SearchResult> results) {
    if (results.isEmpty()) return;
    for (SearchResult r : results) {
      repository.boostPathScore(r.path(), BOOST_PER_HIT, MAX_BOOST);
    }
  }

  public List<String> suggest(String prefix) {
    if (prefix == null || prefix.isBlank()) return List.of();
    String norm = normalise(prefix);
    return frequency.entrySet().stream()
        .filter(e -> e.getKey().startsWith(norm) && !e.getKey().equals(norm))
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(MAX_SUGGESTION)
        .map(Map.Entry::getKey)
        .toList();
  }

  public List<SearchResult> boost(List<SearchResult> results) {
    if (results.isEmpty() || frequency.isEmpty()) return results;
    return results.stream().sorted(Comparator.comparingDouble(this::boostedScore)).toList();
  }

  public long totalSearches() {
    return history.size();
  }

  public List<Map.Entry<String, Long>> topQueries(int n) {
    return frequency.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(n)
        .toList();
  }

  public List<SearchEvent> recentHistory(int n) {
    List<SearchEvent> snap = List.copyOf(history);
    int from = Math.max(0, snap.size() - n);
    return snap.subList(from, snap.size()).reversed();
  }

  private double boostedScore(SearchResult r) {
    double boost = 0.0;
    String lowerName = r.name().toLowerCase();

    for (Map.Entry<String, Long> entry : frequency.entrySet()) {
      String pastQuery = entry.getKey();
      if (!pastQuery.isBlank() && lowerName.contains(pastQuery)) {
        boost += (entry.getValue() * BOOST_PER_HIT);
      }
    }

    return r.score() - Math.min(boost, MAX_BOOST);
  }

  private String normalise(String s) {
    return s.toLowerCase().trim();
  }
}
