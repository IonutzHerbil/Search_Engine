package app.indexer;

import app.config.IndexConfig;
import app.db.DatabaseManager;
import app.model.FileRecord;
import app.model.IndexReport;
import app.model.TraversalStats;
import app.processor.ContentExtractor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class FileIndexer {

    private final IndexConfig config;
    private final FileFilter filter;
    private final ContentExtractor extractor;
    private final DatabaseManager dbManager;

    public FileIndexer(IndexConfig config, DatabaseManager db, FileFilter filter, ContentExtractor extractor) {
        this.config = config;
        this.dbManager = db;
        this.filter = filter;
        this.extractor = extractor;
    }

    public void index() {
        System.out.println("Indexing: " + config.rootDirectory());
        System.out.println("----------------------------------------");

        TraversalStats stats = new TraversalStats();
        Set<Path> visitedRealPaths = new HashSet<>();

        try {
            Files.walkFileTree(config.rootDirectory(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<>() {

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            Path real = dir.toRealPath();
                            if (!visitedRealPaths.add(real)) {
                                stats.recordSkipped();
                                return FileVisitResult.SKIP_SUBTREE;
                            }

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

                            try {
                                String absolutePath=file.toAbsolutePath().toString();
                                long fileModifiedTime=attrs.lastModifiedTime().toMillis();

                                if(fileModifiedTime == dbManager.getLastModified(absolutePath)){
                                    stats.recordSkipped();
                                    return FileVisitResult.CONTINUE;
                                }

                                FileRecord record = extractor.extract(file, attrs);
                                dbManager.upsert(record);
                                stats.recordFile();
                                System.out.println("[SAVED] " + record.name());
                            } catch (Exception e) {
                                stats.recordError();
                            }

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
        System.out.println("========================================");
        System.out.printf("Finished in %.2fs%n", report.elapsedSeconds());
        System.out.printf("Files Indexed : %d%n", report.filesFound());
        System.out.printf("Files Skipped : %d%n", report.skipped());
        System.out.printf("Dirs Visited  : %d%n", report.directoriesVisited());
        System.out.printf("Errors        : %d%n", report.errors());
    }
}