package app.processor;

import app.indexer.PathScorer;
import app.model.FileRecord;
import app.processor.strategy.ExtractionResult;
import app.processor.strategy.ExtractionStrategy;
import app.processor.strategy.ImageExtractionStrategy;
import app.processor.strategy.TextExtractionStrategy;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import org.apache.tika.Tika;

public class ContentExtractor {

  private final Tika tika;
  private final List<ExtractionStrategy> strategies;

  public ContentExtractor() {
    this.tika = new Tika();
    this.tika.setMaxStringLength(5 * 1024 * 1024);
    this.strategies = List.of(new TextExtractionStrategy(tika), new ImageExtractionStrategy());
  }

  public FileRecord extract(Path file, BasicFileAttributes attrs) {
    String name = file.getFileName().toString();
    String ext = app.util.FileTypes.parseExtension(name);
    String content = null;
    String preview = null;
    String dominantColor = null;

    try (InputStream stream = Files.newInputStream(file)) {
      String mimeType = tika.detect(stream);
      for (ExtractionStrategy strategy : strategies) {
        if (strategy.supports(mimeType)) {
          ExtractionResult result = strategy.extract(file, attrs);
          content = result.content();
          preview = result.preview();
          dominantColor = result.dominantColor();
          break;
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
        pathScore,
        dominantColor);
  }
}
