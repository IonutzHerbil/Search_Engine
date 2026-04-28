package app.processor;

import app.indexer.PathScorer;
import app.model.FileRecord;
import app.util.FileTypes;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Collectors;
import org.apache.tika.Tika;

public class ContentExtractor {

  private static final int PREVIEW_LINES = 3;
  private final Tika tika;

  public ContentExtractor() {
    this.tika = new Tika();
    this.tika.setMaxStringLength(5 * 1024 * 1024);
  }

  public FileRecord extract(Path file, BasicFileAttributes attrs) {
    String name = file.getFileName().toString();
    String ext = FileTypes.parseExtension(name);
    String preview = null;
    String content = null;

    try (InputStream stream = Files.newInputStream(file)) {
      String mimeType = tika.detect(stream);

      if (mimeType.startsWith("text/")
          || mimeType.contains("pdf")
          || mimeType.contains("document")) {
        content = tika.parseToString(file);
        if (content != null && !content.isBlank()) {
          preview = extractPreview(content);
        }
      }
    } catch (Exception e) {
      System.err.println("[EXTRACTION WARN] Could not parse " + name + ": " + e.getMessage());
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
