package app.processor;

import app.model.FileRecord;
import app.util.FileTypes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Collectors;

public class ContentExtractor {

  private static final int PREVIEW_LINES = 3;

  public FileRecord extract(Path file, BasicFileAttributes attrs) {
    String name = file.getFileName().toString();
    String ext = parseExtension(name);
    String preview = null;
    String content = null;

    if (FileTypes.isText(ext)) {
      try {
        content = Files.readString(file);
        preview = extractPreview(content);
      } catch (IOException ignored) {
      }
    }

    return new FileRecord(
        file.toAbsolutePath().toString(),
        name,
        ext,
        attrs.size(),
        attrs.lastModifiedTime().toMillis(),
        preview,
        content);
  }

  private String parseExtension(String name) {
    int dot = name.lastIndexOf('.');
    return (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
  }

  private String extractPreview(String text) {
    return text.lines()
        .filter(line -> !line.isBlank())
        .limit(PREVIEW_LINES)
        .collect(Collectors.joining("\n"));
  }
}
