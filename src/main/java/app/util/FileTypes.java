package app.util;

import java.util.Set;

public final class FileTypes {

  private FileTypes() {}

  private static final Set<String> TEXT_EXTENSIONS =
      Set.of(
          "txt",
          "md",
          "java",
          "py",
          "js",
          "ts",
          "html",
          "css",
          "json",
          "xml",
          "csv",
          "sh",
          "bat",
          "c",
          "cpp",
          "h",
          "sql",
          "properties",
          "yml",
          "yaml",
          "text",
          "cs");

  public static boolean isText(String extension) {
    return TEXT_EXTENSIONS.contains(extension.toLowerCase());
  }
}
