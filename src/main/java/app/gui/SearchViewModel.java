package app.gui;

import app.db.FileRepository;
import app.model.SearchResult;
import app.search.SearchEngine;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SearchViewModel {

  private final SearchEngine engine;
  private final FileRepository repository;

  private final ObservableList<SearchResult> results = FXCollections.observableArrayList();
  private final ObservableList<String> availableExtensions = FXCollections.observableArrayList();
  private final StringProperty resultCount = new SimpleStringProperty("");

  private String currentQuery = "";

  public SearchViewModel(SearchEngine engine, FileRepository repository) {
    this.engine = engine;
    this.repository = repository;
  }

  public void search(String terms, String ext, String dir) {
    currentQuery = buildQuery(terms, ext, dir);
    List<SearchResult> found = engine.search(currentQuery);
    results.setAll(found);
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

  private String buildQuery(String terms, String ext, String dir) {
    StringBuilder sb = new StringBuilder(terms);
    if (ext != null && !ext.isBlank()) sb.append(" ext:").append(ext);
    if (dir != null && !dir.isBlank()) sb.append(" dir:").append(dir);
    return sb.toString();
  }

  private void updateCount() {
    int size = results.size();
    resultCount.set(size + " result" + (size == 1 ? "" : "s"));
  }
}
