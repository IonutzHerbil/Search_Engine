package app.gui;

import app.indexer.IndexerFactory;
import app.model.IndexReport;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

public class IndexViewModel {

  private final IndexerFactory factory;

  private final StringProperty status = new SimpleStringProperty("Ready");
  private final BooleanProperty indexing = new SimpleBooleanProperty(false);
  private final ObjectProperty<IndexReport> report = new SimpleObjectProperty<>(null);

  private Runnable onIndexComplete;

  public IndexViewModel(IndexerFactory factory) {
    this.factory = factory;
  }

  public void setOnIndexComplete(Runnable callback) {
    this.onIndexComplete = callback;
  }

  public void index(String path, Set<String> ignoredDirs, Set<String> ignoredExts) {
    if (path == null || path.isBlank()) {
      status.set("Enter a directory path first.");
      return;
    }

    var indexer = factory.create(path, ignoredDirs, ignoredExts);
    indexing.set(true);
    report.set(null);

    Task<IndexReport> task =
        new Task<>() {
          @Override
          protected IndexReport call() {
            return indexer.index(
                name ->
                    javafx.application.Platform.runLater(() -> status.set("Indexing: " + name)));
          }
        };

    task.setOnRunning(e -> status.set("Indexing…"));
    task.setOnSucceeded(
        e -> {
          report.set(task.getValue());
          indexing.set(false);
          status.set("Done.");
          if (onIndexComplete != null) onIndexComplete.run();
        });
    task.setOnFailed(
        e -> {
          indexing.set(false);
          status.set("Failed: " + task.getException().getMessage());
        });

    Thread t = new Thread(task);
    t.setDaemon(true);
    t.start();
  }

  public boolean isIndexing() {
    return indexing.get();
  }

  public StringProperty statusProperty() {
    return status;
  }

  public BooleanProperty indexingProperty() {
    return indexing;
  }

  public ObjectProperty<IndexReport> reportProperty() {
    return report;
  }

  public void setStatus(String message) {
    status.set(message);
  }
}
