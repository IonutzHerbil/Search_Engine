package app.indexer;

import app.db.FileRepository;
import app.model.FileRecord;
import app.processor.ContentExtractor;
import app.util.FileTypes;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileWatcher {

  private final Path root;
  private final FileRepository repository;
  private final ContentExtractor extractor;
  private final FileFilter filter;
  private final Map<WatchKey, Path> keyToDir = new HashMap<>();

  private WatchService watchService;
  private ExecutorService executor;

  public FileWatcher(
      Path root, FileRepository repository, ContentExtractor extractor, FileFilter filter) {
    this.root = root;
    this.repository = repository;
    this.extractor = extractor;
    this.filter = filter;
  }

  public void start() {
    try {
      watchService = FileSystems.getDefault().newWatchService();
      registerAll(root);
      executor =
          Executors.newSingleThreadExecutor(
              r -> {
                Thread t = new Thread(r, "file-watcher");
                t.setDaemon(true);
                return t;
              });
      executor.submit(this::watchLoop);
    } catch (IOException e) {
      System.err.println("[WATCHER] Failed to start: " + e.getMessage());
    }
  }

  public void stop() {
    if (executor != null) executor.shutdownNow();
    if (watchService != null) {
      try {
        watchService.close();
      } catch (IOException ignored) {
      }
    }
  }

  private void watchLoop() {
    while (!Thread.currentThread().isInterrupted()) {
      WatchKey key;
      try {
        key = watchService.poll(1, TimeUnit.SECONDS);
        if (key != null) {
          System.out.println("[WATCHER] Got event from: " + keyToDir.get(key));
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      if (key == null) continue;

      Path dir = keyToDir.get(key);
      if (dir == null) {
        key.reset();
        continue;
      }

      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind<?> kind = event.kind();
        if (kind == StandardWatchEventKinds.OVERFLOW) continue;

        Path file = dir.resolve((Path) event.context()).toAbsolutePath();

        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
          repository.deletePath(file.toString());
          continue;
        }

        if (Files.isDirectory(file)) {
          try {
            registerAll(file);
          } catch (IOException ignored) {
          }
          continue;
        }

        if (!Files.isRegularFile(file)) continue;
        String ext = FileTypes.parseExtension(file.getFileName().toString());
        if (filter.shouldSkipFile(ext)) continue;

        try {
          Thread.sleep(200);
          BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
          FileRecord record = extractor.extract(file, attrs);
          System.out.println("[WATCHER] Processing: " + file);
          repository.upsertNoCommit(record);
          repository.commit();
          System.out.println("[WATCHER] Indexed: " + file.getFileName());
        } catch (IOException | InterruptedException e) {
          System.err.println("[WATCHER] Error processing " + file + ": " + e.getMessage());
        }
      }

      key.reset();
    }
  }

  private void registerAll(Path start) throws IOException {
    Files.walkFileTree(
        start,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
            if (filter.shouldSkipDir(name)) return FileVisitResult.SKIP_SUBTREE;
            WatchKey key =
                dir.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            keyToDir.put(key, dir);
            System.out.println("[WATCHER] Registering: " + dir);
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
