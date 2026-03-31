package app.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

public record IndexConfig(
    Path rootDirectory,
    Set<String> ignoredDirectoryPatterns,
    Set<String> ignoredExtensions,
    String dbPath,
    String reportFormat) {

  public static final Set<String> DEFAULT_IGNORED_DIR_NAMES =
      Set.of(
          ".hidden",
          ".idea",
          ".git",
          "node_modules",
          "target",
          "build",
          "cache",
          "dist",
          "env",
          "venv",
          "AppData",
          "flutter",
          "scoop",
          "__pycache__");

  public static final Set<String> DEFAULT_IGNORED_EXTS =
      Set.of(
          "class",
          "obj",
          "o",
          "pyc",
          "pyo",
          "zip",
          "tar",
          "gz",
          "rar",
          "7z",
          "tmp",
          "bak",
          "lnk",
          "db",
          "sqlite",
          "lock",
          "iml",
          "suo",
          "user",
          "map",
          "DS_Store");

  private static final Set<String> DEFAULT_IGNORED_DIRS =
      DEFAULT_IGNORED_DIR_NAMES.stream()
          .map(name -> name.startsWith(".") ? "^\\" + name + ".*" : ".*" + name + ".*")
          .collect(Collectors.toUnmodifiableSet());

  public static IndexConfig fromArgs(String[] args) {
    Path root = args.length > 0 ? Paths.get(args[0]) : Paths.get(System.getProperty("user.home"));
    String db = args.length > 1 ? args[1] : "vision_index.db";
    String format = args.length > 2 ? args[2].toUpperCase() : "TEXT";
    return new IndexConfig(root, DEFAULT_IGNORED_DIRS, DEFAULT_IGNORED_EXTS, db, format);
  }

  public static IndexConfig custom(String path, Set<String> ignoredDirs, Set<String> ignoredExts) {
    return new IndexConfig(Paths.get(path), ignoredDirs, ignoredExts, "vision_index.db", "TEXT");
  }
}
