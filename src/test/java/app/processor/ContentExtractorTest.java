package app.processor;

import static org.junit.jupiter.api.Assertions.*;

import app.model.FileRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContentExtractorTest {

  private ContentExtractor extractor;

  @BeforeEach
  void setUp() {
    extractor = new ContentExtractor();
  }

  @Test
  void testExtractsTextAndStripsMarkup(@TempDir Path tempDir) throws Exception {
    Path testFile = tempDir.resolve("test.html");
    String htmlContent =
        "<html><body><h1>Hello World</h1><p>This is a <b>Tika</b> test.</p></body></html>";
    Files.writeString(testFile, htmlContent);
    BasicFileAttributes attrs = Files.readAttributes(testFile, BasicFileAttributes.class);

    FileRecord record = extractor.extract(testFile, attrs);

    assertNotNull(record.content());
    assertTrue(record.content().contains("Hello World"));
    assertTrue(record.content().contains("This is a Tika test."));
    assertFalse(record.content().contains("<h1>"));
    assertNotNull(record.preview());
    assertTrue(record.preview().contains("Hello World"));
  }

  @Test
  void testGracefullySkipsUnknownBinaries(@TempDir Path tempDir) throws Exception {
    Path binFile = tempDir.resolve("fake_program.bin");
    Files.write(binFile, new byte[] {0x00, 0x01, 0x02, 0x03});
    BasicFileAttributes attrs = Files.readAttributes(binFile, BasicFileAttributes.class);

    FileRecord record = extractor.extract(binFile, attrs);

    assertNull(record.content());
    assertNull(record.preview());
    assertEquals("fake_program.bin", record.name());
  }
}
