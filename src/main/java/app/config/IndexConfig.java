package app.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public record IndexConfig(
    Path rootDirectory,
    Set<String> ignoredDirectoryPatterns,
    Set<String> ignoredExtensions,
    String dbPath,
    String reportFormat) {
  private static final Set<String> DEFAULT_IGNORED_DIRS =
      Set.of(
          "^\\..*",
          ".*node_modules.*",
          ".*target.*",
          ".*build.*",
          ".*cache.*",
          ".*dist.*",
          ".*env.*",
          ".*flutter.*",
          ".*scoop.*",
          ".*AppData.*");

  private static final Set<String> DEFAULT_IGNORED_EXTS =
      Set.of(
          "exe",
          "dll",
          "so",
          "dylib",
          "o",
          "obj",
          "class",
          "jar",
          "zip",
          "tar",
          "gz",
          "rar",
          "7z",
          "png",
          "jpg",
          "jpeg",
          "gif",
          "bmp",
          "ico",
          "svg",
          "webp",
          "mp3",
          "mp4",
          "avi",
          "mov",
          "mkv",
          "wav",
          "pdf",
          "doc",
          "docx",
          "xls",
          "xlsx",
          "ppt",
          "pptx",
          "fbx",
          "max",
          "uasset",
          "umap",
          "uproject",
          "blend",
          "bin",
          "dmp",
          "ush",
          "usf",
          "pak",
          "dat",
          "bak",
          "db",
          "sqlite",
          "lnk",
          "ini",
          "log",
          "tmp");

  public static IndexConfig fromArgs(String[] args) {
    Path root = args.length > 0 ? Paths.get(args[0]) : Paths.get(System.getProperty("user.home"));
    String db = args.length > 1 ? args[1] : "vision_index.db";
    String format = args.length > 2 ? args[2].toUpperCase() : "TEXT";

    return new IndexConfig(root, DEFAULT_IGNORED_DIRS, DEFAULT_IGNORED_EXTS, db, format);
  }
}
