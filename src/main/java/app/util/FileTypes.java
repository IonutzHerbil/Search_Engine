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

  private static final Set<String> IMAGE_EXTENSIONS =
      Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif", "ico");

  public static boolean isText(String extension) {
    return TEXT_EXTENSIONS.contains(extension.toLowerCase());
  }

  public static boolean isImage(String extension) {
    return IMAGE_EXTENSIONS.contains(extension.toLowerCase());
  }

  public static String parseExtension(String name) {
    int dot = name.lastIndexOf('.');
    return (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1).toLowerCase() : "";
  }
}
