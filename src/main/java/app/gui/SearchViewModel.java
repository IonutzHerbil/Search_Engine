package app.gui;

import app.db.FileRepository;
import app.model.SearchResult;
import app.search.SearchEngine;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SearchViewModel {

  private static final int PAGE_SIZE = 50;

  private final SearchEngine engine;
  private final FileRepository repository;

  private final ObservableList<SearchResult> results = FXCollections.observableArrayList();
  private final ObservableList<String> availableExtensions = FXCollections.observableArrayList();
  private final StringProperty resultCount = new SimpleStringProperty("");
  private final BooleanProperty hasMore = new SimpleBooleanProperty(false);

  private String currentQuery = "";
  private int currentOffset = 0;

  public SearchViewModel(SearchEngine engine, FileRepository repository) {
    this.engine = engine;
    this.repository = repository;
  }

  public void search(String terms, String ext, String dir) {
    currentQuery = buildQuery(terms, ext, dir);
    currentOffset = 0;
    List<SearchResult> found = engine.search(currentQuery, PAGE_SIZE, 0);
    results.setAll(found);
    currentOffset = found.size();
    hasMore.set(found.size() == PAGE_SIZE);
    updateCount();
  }

  public void loadMore() {
    if (!hasMore.get() || currentQuery.isBlank()) return;
    List<SearchResult> more = engine.search(currentQuery, PAGE_SIZE, currentOffset);
    results.addAll(more);
    currentOffset += more.size();
    hasMore.set(more.size() == PAGE_SIZE);
    updateCount();
  }

  public void refreshExtensions() {
    availableExtensions.setAll(repository.getDistinctExtensions());
  }

  public ObservableList<SearchResult> getResults() {
    return results;
  }

  public ObservableList<String> getAvailableExtensions() {
    return availableExtensions;
  }

  public StringProperty resultCountProperty() {
    return resultCount;
  }

  public BooleanProperty hasMoreProperty() {
    return hasMore;
  }

  private String buildQuery(String terms, String ext, String dir) {
    StringBuilder sb = new StringBuilder(terms);
    if (ext != null && !ext.isBlank()) sb.append(" ext:").append(ext);
    if (dir != null && !dir.isBlank()) sb.append(" dir:").append(dir);
    return sb.toString();
  }

  private void updateCount() {
    int size = results.size();
    resultCount.set(size + " result" + (size == 1 ? "" : "s") + (hasMore.get() ? "+" : ""));
  }
}
