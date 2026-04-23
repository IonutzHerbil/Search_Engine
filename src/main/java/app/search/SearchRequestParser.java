package app.search;

public class SearchRequestParser {

  private static final String EXT_PREFIX = "ext:";
  private static final String PATH_PREFIX = "path:";
  private static final String CONTENT_PREFIX = "content:";
  private static final String NAME_PREFIX = "name:";

  public SearchRequest parse(String raw) {
    String[] parts = raw.trim().split("\\s+");

    StringBuilder ftsTerms = new StringBuilder();
    StringBuilder pathValue = new StringBuilder();
    StringBuilder contentValue = new StringBuilder();
    String extension = null;
    String activeQualifier = null;

    for (String part : parts) {
      String lower = part.toLowerCase();

      if (lower.startsWith(EXT_PREFIX)) {
        activeQualifier = null;
        extension = part.substring(EXT_PREFIX.length()).toLowerCase();

      } else if (lower.startsWith(PATH_PREFIX)) {
        activeQualifier = "path";
        String val = part.substring(PATH_PREFIX.length());
        if (!val.isBlank()) {
          if (!pathValue.isEmpty()) pathValue.append(" ");
          pathValue.append(val);
        }

      } else if (lower.startsWith(CONTENT_PREFIX)) {
        activeQualifier = "content";
        String val = sanitize(part.substring(CONTENT_PREFIX.length()));
        if (!val.isBlank()) {
          if (!contentValue.isEmpty()) contentValue.append(" ");
          contentValue.append(val);
        }

      } else if (lower.startsWith(NAME_PREFIX)) {
        activeQualifier = null;
        String val = sanitize(part.substring(NAME_PREFIX.length()));
        if (!val.isBlank()) {
          if (!ftsTerms.isEmpty()) ftsTerms.append(" ");
          ftsTerms.append("name:").append(val);
        }

      } else if ("path".equals(activeQualifier)) {
        if (!pathValue.isEmpty()) pathValue.append(" ");
        pathValue.append(part);

      } else if ("content".equals(activeQualifier)) {
        String val = sanitize(part);
        if (!val.isBlank()) {
          if (!contentValue.isEmpty()) contentValue.append(" ");
          contentValue.append(val);
        }

      } else {
        String val = sanitize(part);
        if (!val.isBlank()) {
          if (!ftsTerms.isEmpty()) ftsTerms.append(" ");
          ftsTerms.append(val);
        }
      }
    }

    if (!contentValue.isEmpty()) {
      if (!ftsTerms.isEmpty()) ftsTerms.append(" ");
      ftsTerms.append("content:").append(contentValue);
    }

    String directory = pathValue.isEmpty() ? null : pathValue.toString().trim();
    return new SearchRequest(ftsTerms.toString(), extension, directory);
  }

  private String sanitize(String s) {
    return s.replaceAll("[^\\p{L}\\p{N}\\s]", " ").trim();
  }
}
