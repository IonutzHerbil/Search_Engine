package app.gui;

import app.db.FileRepository;
import app.model.SearchResult;
import app.search.SearchEngine;
import app.search.SortOrder;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
  private final ObjectProperty<SortOrder> sortOrder =
      new SimpleObjectProperty<>(SortOrder.RELEVANCE);

  private String currentQuery = "";
  private int currentOffset = 0;

  public SearchViewModel(SearchEngine engine, FileRepository repository) {
    this.engine = engine;
    this.repository = repository;
  }

  public void search(String terms, String ext, String dir) {
    currentQuery = buildQuery(terms, ext, dir);
    currentOffset = 0;
    String query = currentQuery;
    SortOrder sort = sortOrder.get();
    Thread.ofVirtual()
        .start(
            () -> {
              List<SearchResult> found = engine.search(query, PAGE_SIZE, 0, sort);
              javafx.application.Platform.runLater(
                  () -> {
                    results.setAll(found);
                    currentOffset = found.size();
                    hasMore.set(found.size() == PAGE_SIZE);
                    updateCount();
                  });
            });
  }

  public void loadMore() {
    if (!hasMore.get() || currentQuery.isBlank()) return;
    String query = currentQuery;
    int offset = currentOffset;
    SortOrder sort = sortOrder.get();
    Thread.ofVirtual()
        .start(
            () -> {
              List<SearchResult> more = engine.search(query, PAGE_SIZE, offset, sort);
              javafx.application.Platform.runLater(
                  () -> {
                    results.addAll(more);
                    currentOffset += more.size();
                    hasMore.set(more.size() == PAGE_SIZE);
                    updateCount();
                  });
            });
  }

  public void refreshExtensions() {
    availableExtensions.setAll(repository.getDistinctExtensions());
  }

  public void setSortOrder(SortOrder order) {
    sortOrder.set(order);
  }

  public ObjectProperty<SortOrder> sortOrderProperty() {
    return sortOrder;
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
    if (dir != null && !dir.isBlank()) sb.append(" path:").append(dir);
    return sb.toString();
  }

  private void updateCount() {
    int size = results.size();
    resultCount.set(size + " result" + (size == 1 ? "" : "s") + (hasMore.get() ? "+" : ""));
  }
}
