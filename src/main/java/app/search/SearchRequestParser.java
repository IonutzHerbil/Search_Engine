package app.search;

public class SearchRequestParser {

  private static final String EXT_PREFIX = "ext:";
  private static final String DIR_PREFIX = "dir:";

  public SearchRequest parse(String raw) {
    String[] parts = raw.trim().split("\\s+");
    StringBuilder terms = new StringBuilder();
    String extension = null;
    String directory = null;

    for (String part : parts) {
      if (part.toLowerCase().startsWith(EXT_PREFIX)) {
        extension = part.substring(EXT_PREFIX.length()).toLowerCase();
      } else if (part.toLowerCase().startsWith(DIR_PREFIX)) {
        directory = part.substring(DIR_PREFIX.length());
      } else {
        if (!terms.isEmpty()) terms.append(" ");
        terms.append(part);
      }
    }

    String sanitized = terms.toString().replaceAll("[^\\p{L}\\p{N}\\s]", " ").trim();

    return new SearchRequest(sanitized, extension, directory);
  }
}
