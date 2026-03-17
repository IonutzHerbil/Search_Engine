package vision.indexer;

import vision.config.IndexConfig;
import vision.model.IndexReport;
import vision.model.TraversalStats;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

public class FileIndexer {

    private final IndexConfig config;
    private final FileFilter filter;

    public FileIndexer(IndexConfig config) {
        this.config = config;
        this.filter = new FileFilter(config);
    }

    public void index() {
        System.out.println("Indexing: " + config.rootDirectory());
        System.out.println("----------------------------------------");

        TraversalStats stats = new TraversalStats();

        try {
            Files.walkFileTree(config.rootDirectory(), new SimpleFileVisitor<>() {

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                            if (filter.shouldSkipDir(name)) {
                                stats.recordSkipped();
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            stats.recordDirectory();
                            return FileVisitResult.CONTINUE;
                        }
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            String name = file.getFileName().toString();
                            int dot = name.lastIndexOf('.');
                            String ext = (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";

                            if (filter.shouldSkipFile(ext)) {
                                stats.recordSkipped();
                                return FileVisitResult.CONTINUE;
                            }
                            System.out.printf("[FILE] %s (%d bytes)%n", file, attrs.size());
                            stats.recordFile();
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

        IndexReport report = stats.toReport(config.rootDirectory().toString());
        System.out.println("Finished indexing: " + report.filesFound() + " files in " + report.elapsedSeconds() + "s");    }
}