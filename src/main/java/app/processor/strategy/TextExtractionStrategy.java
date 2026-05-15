package app.processor.strategy;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Collectors;
import org.apache.tika.Tika;

public class TextExtractionStrategy implements ExtractionStrategy {

  private static final int PREVIEW_LINES = 3;
  private final Tika tika;

  public TextExtractionStrategy(Tika tika) {
    this.tika = tika;
  }

  @Override
  public boolean supports(String mimeType) {
    return mimeType.startsWith("text/")
        || mimeType.contains("pdf")
        || mimeType.contains("document");
  }

  @Override
  public ExtractionResult extract(Path file, BasicFileAttributes attrs) throws Exception {
    String content = tika.parseToString(file);
    if (content == null || content.isBlank()) return new ExtractionResult(null, null, null);
    String preview =
        content
            .lines()
            .filter(line -> !line.isBlank())
            .limit(PREVIEW_LINES)
            .collect(Collectors.joining("\n"));
    return new ExtractionResult(content, preview, null);
  }
}
