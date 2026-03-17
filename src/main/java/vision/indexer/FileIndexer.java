package vision.indexer;

import vision.model.IndexReport;
import vision.model.TraversalStats;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileIndexer {

    private final Path rootDirectory;

    public FileIndexer(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public void index() {
        System.out.println("Indexing: " + rootDirectory);
        System.out.println("----------------------------------------");

        TraversalStats stats = new TraversalStats();

        try {
            Files.walkFileTree(rootDirectory, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (name.startsWith(".")) {
                        stats.recordSkipped();
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    stats.recordDirectory();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    stats.recordFile();
                    System.out.printf("[FILE] %s (%d bytes)%n", file, attrs.size());
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

        IndexReport report = stats.toReport(rootDirectory.toString());
        System.out.println("Finished indexing: " + report.filesFound() + " files in " + report.elapsedSeconds() + "s");    }
}