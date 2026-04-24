package app.search;

import java.util.ArrayList;
import java.util.List;

public class SearchRequestParser {

  private static final String EXT_PREFIX = "ext:";
  private static final String PATH_PREFIX = "path:";
  private static final String CONTENT_PREFIX = "content:";
  private static final String NAME_PREFIX = "name:";

  public SearchRequest parse(String raw) {
    String[] parts = raw.trim().split("\\s+");

    StringBuilder ftsTerms = new StringBuilder();
    StringBuilder contentValue = new StringBuilder();
    List<StringBuilder> pathSegments = new ArrayList<>();
    List<String> extensions = new ArrayList<>();
    String activeQualifier = null;

    for (String part : parts) {
      String lower = part.toLowerCase();

      if (lower.startsWith(EXT_PREFIX)) {
        activeQualifier = null;
        String val = part.substring(EXT_PREFIX.length()).toLowerCase();
        if (!val.isBlank()) extensions.add(val);

      } else if (lower.startsWith(PATH_PREFIX)) {
        pathSegments.add(new StringBuilder());
        activeQualifier = "path";
        String val = part.substring(PATH_PREFIX.length());
        if (!val.isBlank()) pathSegments.getLast().append(val);

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
        StringBuilder current = pathSegments.getLast();
        if (!current.isEmpty()) current.append(" ");
        current.append(part);

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

    List<String> directories =
        pathSegments.stream().map(sb -> sb.toString().trim()).filter(s -> !s.isBlank()).toList();

    return new SearchRequest(ftsTerms.toString(), extensions, directories);
  }

  private String sanitize(String s) {
    return s.replaceAll("[^\\p{L}\\p{N}\\s]", " ").trim();
  }
}
