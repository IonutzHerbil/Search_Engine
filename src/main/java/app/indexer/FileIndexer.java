package app.indexer;

import app.config.IndexConfig;
import app.db.FileRepository;
import app.model.FileRecord;
import app.model.IndexReport;
import app.model.TraversalStats;
import app.processor.ContentExtractor;
import app.util.FileTypes;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class FileIndexer {

  private final IndexConfig config;
  private final FileFilter filter;
  private final ContentExtractor extractor;
  private final FileRepository repository;

  private static final int COMMIT_BATCH_SIZE = 200;
  private static final int READER_THREADS = Math.min(Runtime.getRuntime().availableProcessors(), 8);
  private static final int QUEUE_CAPACITY = 500;

  private static final FileRecord POISON_PILL =
      new FileRecord("__POISON__", null, null, 0, 0, null, null, 0.0, null);

  public FileIndexer(
      IndexConfig config,
      FileRepository repository,
      FileFilter filter,
      ContentExtractor extractor) {
    this.config = config;
    this.repository = repository;
    this.filter = filter;
    this.extractor = extractor;
  }

  public IndexReport index(Consumer<String> onFileIndexed) {
    TraversalStats stats = new TraversalStats();
    Set<Path> visitedRealPaths = new HashSet<>();
    Map<String, Long> lastModifiedCache =
        repository.getLastModifiedMap(config.rootDirectory().toAbsolutePath().toString());

    AtomicInteger newFiles = new AtomicInteger(0);
    AtomicInteger updatedFiles = new AtomicInteger(0);
    AtomicInteger producerErrors = new AtomicInteger(0);

    ExecutorService producers = Executors.newFixedThreadPool(READER_THREADS);

    BlockingQueue<FileRecord> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    Thread consumer =
        new Thread(
            () -> {
              int pending = 0;
              try {
                while (true) {
                  FileRecord record = queue.take();
                  if (record == POISON_PILL) break;
                  repository.upsertNoCommit(record);
                  if (++pending >= COMMIT_BATCH_SIZE) {
                    repository.commit();
                    pending = 0;
                  }
                  onFileIndexed.accept(record.name());
                }
                if (pending > 0) repository.commit();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            },
            "index-writer");
    consumer.start();

    try {
      Files.walkFileTree(
          config.rootDirectory(),
          EnumSet.of(FileVisitOption.FOLLOW_LINKS),
          Integer.MAX_VALUE,
          new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
              Path real = dir.toRealPath();
              if (!visitedRealPaths.add(real)) {
                stats.recordFiltered();
                return FileVisitResult.SKIP_SUBTREE;
              }
              String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
              if (filter.shouldSkipDir(name)) {
                stats.recordFiltered();
                return FileVisitResult.SKIP_SUBTREE;
              }
              stats.recordDirectory();
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
              String name = file.getFileName().toString();
              String ext = FileTypes.parseExtension(name);
              if (filter.shouldSkipFile(ext)) {
                stats.recordFiltered();
                return FileVisitResult.CONTINUE;
              }

              String absolutePath = file.toAbsolutePath().toString();
              long fileModifiedTime = attrs.lastModifiedTime().toMillis();
              long storedModified = lastModifiedCache.getOrDefault(absolutePath, -1L);

              if (fileModifiedTime == storedModified) {
                stats.recordUpToDate();
                return FileVisitResult.CONTINUE;
              }

              boolean isNew = storedModified == -1;

              producers.submit(
                  () -> {
                    try {
                      FileRecord record = extractor.extract(file, attrs);
                      queue.put(record);
                      if (isNew) newFiles.incrementAndGet();
                      else updatedFiles.incrementAndGet();
                    } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                      producerErrors.incrementAndGet();
                    } catch (Exception e) {
                      producerErrors.incrementAndGet();
                      System.err.println("[PRODUCER ERROR] " + file + ": " + e.getMessage());
                    }
                  });

              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
              stats.recordError();
              System.err.println("[ERROR] " + file + " -> " + exc.getMessage());
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      System.err.println("[FATAL] Cannot start traversal: " + e.getMessage());
    }

    producers.shutdown();
    try {
      producers.awaitTermination(1, TimeUnit.HOURS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    try {
      queue.put(POISON_PILL);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    try {
      consumer.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    repository.deleteStale(config.rootDirectory().toString());

    for (int i = 0; i < newFiles.get(); i++) stats.recordNewFile();
    for (int i = 0; i < updatedFiles.get(); i++) stats.recordUpdatedFile();
    for (int i = 0; i < producerErrors.get(); i++) stats.recordError();

    return stats.toReport(config.rootDirectory().toString());
  }

  public Path getRootDirectory() {
    return config.rootDirectory();
  }
}
