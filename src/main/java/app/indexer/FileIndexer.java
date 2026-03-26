package app.indexer;

import app.config.IndexConfig;
import app.db.FileRepository;
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
    private final FileRepository repository;

    public FileIndexer(IndexConfig config, FileRepository repository,FileFilter filter, ContentExtractor extractor) {
        this.config     = config;
        this.repository = repository;
        this.filter     = filter;
        this.extractor  = extractor;
    }

    public IndexReport index() {
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

                                if(fileModifiedTime == repository.getLastModified(absolutePath)){
                                    stats.recordSkipped();
                                    return FileVisitResult.CONTINUE;
                                }

                                FileRecord record = extractor.extract(file, attrs);
                                repository.upsert(record);
                                stats.recordFile();
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

        repository.deleteStale(config.rootDirectory().toString());
        return stats.toReport(config.rootDirectory().toString());
    }
}