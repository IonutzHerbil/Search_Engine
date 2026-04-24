package app.processor;

import app.indexer.PathScorer;
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
    String ext = FileTypes.parseExtension(name);
    String preview = null;
    String content = null;

    if (FileTypes.isText(ext)) {
      try {
        content = Files.readString(file);
        preview = extractPreview(content);
      } catch (IOException ignored) {
      }
    }

    double pathScore = PathScorer.score(file.toAbsolutePath().toString(), ext);

    return new FileRecord(
        file.toAbsolutePath().toString(),
        name,
        ext,
        attrs.size(),
        attrs.lastModifiedTime().toMillis(),
        preview,
        content,
        pathScore);
  }

  private String extractPreview(String text) {
    return text.lines()
        .filter(line -> !line.isBlank())
        .limit(PREVIEW_LINES)
        .collect(Collectors.joining("\n"));
  }
}
