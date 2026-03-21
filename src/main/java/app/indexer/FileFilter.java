package app.indexer;

import app.config.IndexConfig;

import java.util.List;
import java.util.regex.Pattern;

public class FileFilter {

    private final IndexConfig config;
    private final List<Pattern> dirPatterns;

    public FileFilter(IndexConfig config) {
        this.config = config;
        this.dirPatterns = config.ignoredDirectoryPatterns().stream()
                .map(regex -> Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
                .toList();
    }

    public boolean shouldSkipDir(String name) {
        return dirPatterns.stream().anyMatch(pattern -> pattern.matcher(name).matches());
    }

    public boolean shouldSkipFile(String extension) {
        return config.ignoredExtensions().contains(extension.toLowerCase());
    }
}