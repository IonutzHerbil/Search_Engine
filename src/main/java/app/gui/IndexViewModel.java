package app.gui;

import app.config.IndexConfig;
import app.db.FileRepository;
import app.indexer.FileFilter;
import app.indexer.FileWatcher;
import app.indexer.IndexerFactory;
import app.model.IndexReport;
import app.processor.ContentExtractor;
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
  private final FileRepository repository;
  private final ContentExtractor extractor;

  private final StringProperty status = new SimpleStringProperty("Ready");
  private final BooleanProperty indexing = new SimpleBooleanProperty(false);
  private final ObjectProperty<IndexReport> report = new SimpleObjectProperty<>(null);

  private Runnable onIndexComplete;
  private FileWatcher activeWatcher;

  public IndexViewModel(
      IndexerFactory factory, FileRepository repository, ContentExtractor extractor) {
    this.factory = factory;
    this.repository = repository;
    this.extractor = extractor;
  }

  public void setOnIndexComplete(Runnable callback) {
    this.onIndexComplete = callback;
  }

  public void index(String path, Set<String> ignoredDirs, Set<String> ignoredExts) {
    if (path == null || path.isBlank()) {
      status.set("Enter a directory path first.");
      return;
    }

    if (activeWatcher != null) activeWatcher.stop();

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
          status.set("Watching for changes…");
          if (onIndexComplete != null) onIndexComplete.run();

          System.out.println("[WATCHER] Starting watcher for: " + path);
          IndexConfig config = IndexConfig.custom(path, ignoredDirs, ignoredExts);
          FileFilter filter = new FileFilter(config);
          activeWatcher = new FileWatcher(config.rootDirectory(), repository, extractor, filter);
          activeWatcher.start();
          System.out.println("[WATCHER] Watcher started");
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

  public void stopWatcher() {
    if (activeWatcher != null) activeWatcher.stop();
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
