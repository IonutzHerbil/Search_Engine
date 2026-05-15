package app.processor.strategy;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public interface ExtractionStrategy {
  boolean supports(String mimeType);

  ExtractionResult extract(Path file, BasicFileAttributes attrs) throws Exception;
}
