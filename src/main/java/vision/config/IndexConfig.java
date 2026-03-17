package vision.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public record IndexConfig(
        Path        rootDirectory,
        Set<String> ignoredDirectoryPatterns,
        Set<String> ignoredExtensions,
        String      dbPath,
        String      reportFormat
) {
    private static final Set<String> DEFAULT_IGNORED_DIRS = Set.of(
            "^\\..*",
            ".*node_modules.*",
            ".*target.*",
            ".*build.*",
            ".*cache.*",
            ".*dist.*",
            ".*env.*",
            ".*flutter.*",
            ".*scoop.*",
            ".*AppData.*"
    );

    private static final Set<String> DEFAULT_IGNORED_EXTS = Set.of(
            "exe", "dll", "so", "dylib", "o", "obj", "class", "jar",
            "zip", "tar", "gz", "rar", "7z",
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "svg",
            "mp3", "mp4", "avi", "mov", "mkv", "wav","dat","bak","blf","regtrans-ms","lnk","ini","log"
    );

    public static IndexConfig fromArgs(String[] args) {
        Path root = args.length > 0 ? Paths.get(args[0]) : Paths.get(System.getProperty("user.home"));
        String db = args.length > 1 ? args[1] : "vision_index.db";
        String format = args.length > 2 ? args[2].toUpperCase() : "TEXT";

        return new IndexConfig(root, DEFAULT_IGNORED_DIRS, DEFAULT_IGNORED_EXTS, db, format);
    }
}