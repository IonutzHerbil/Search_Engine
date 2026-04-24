package app.search;

import app.model.SearchResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SearchHistoryService implements SearchObserver {

  private static final int MAX_HISTORY = 500;
  private static final int MAX_SUGGESTION = 5;
  private static final double BOOST_PER_HIT = 0.05;

  private final List<SearchEvent> history = new CopyOnWriteArrayList<>();
  private final Map<String, Long> frequency = new ConcurrentHashMap<>();

  @Override
  public void onSearch(SearchEvent event) {
    if (history.size() >= MAX_HISTORY) history.remove(0);
    history.add(event);
    frequency.merge(normalise(event.query()), 1L, Long::sum);
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

  private double boostedScore(SearchResult r) {
    long freq = frequency.getOrDefault(r.name().toLowerCase(), 0L);
    return r.score() - (freq * BOOST_PER_HIT);
  }

  private String normalise(String s) {
    return s.toLowerCase().trim();
  }
}
