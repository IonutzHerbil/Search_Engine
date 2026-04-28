package app.search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchRequestParser {

  private static final String EXT_PREFIX = "ext:";
  private static final String PATH_PREFIX = "path:";
  private static final String CONTENT_PREFIX = "content:";
  private static final String NAME_PREFIX = "name:";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("\"[^\"]+\"|AND|OR|NOT|\\w+:[^\\s]+|\\w+\\*?|\\S+");

  public SearchRequest parse(String raw) {
    List<String> tokens = tokenise(raw.trim());
    StringBuilder ftsTerms = new StringBuilder();
    StringBuilder contentValue = new StringBuilder();
    List<StringBuilder> pathSegments = new ArrayList<>();
    List<String> extensions = new ArrayList<>();
    String activeQualifier = null;

    for (String token : tokens) {
      String lower = token.toLowerCase();

      if (lower.startsWith(EXT_PREFIX)) {
        activeQualifier = null;
        String val = token.substring(EXT_PREFIX.length()).toLowerCase();
        if (!val.isBlank()) extensions.add(val);

      } else if (lower.startsWith(PATH_PREFIX)) {
        pathSegments.add(new StringBuilder());
        activeQualifier = "path";
        String val = token.substring(PATH_PREFIX.length());
        if (!val.isBlank()) pathSegments.getLast().append(val);

      } else if (lower.startsWith(CONTENT_PREFIX)) {
        activeQualifier = "content";
        String val = token.substring(CONTENT_PREFIX.length());
        if (!val.isBlank()) {
          if (!contentValue.isEmpty()) contentValue.append(" ");
          contentValue.append(val);
        }

      } else if (lower.startsWith(NAME_PREFIX)) {
        activeQualifier = null;
        String val = token.substring(NAME_PREFIX.length());
        if (!val.isBlank()) {
          if (!ftsTerms.isEmpty()) ftsTerms.append(" ");
          ftsTerms.append("name:").append(val);
        }

      } else if ("path".equals(activeQualifier)) {
        StringBuilder current = pathSegments.getLast();
        if (!current.isEmpty()) current.append(" ");
        current.append(token);

      } else if ("content".equals(activeQualifier)) {
        if (!contentValue.isEmpty()) contentValue.append(" ");
        contentValue.append(token);

      } else {
        if (token.startsWith("\"") && token.endsWith("\"")) {
          String inner =
              token.substring(1, token.length() - 1).replaceAll("[^\\p{L}\\p{N}\\s]", " ").trim();
          if (!inner.isBlank()) {
            if (!ftsTerms.isEmpty()) ftsTerms.append(" ");
            ftsTerms.append("\"").append(inner).append("\"");
          }
        } else {
          if (!ftsTerms.isEmpty()) ftsTerms.append(" ");
          ftsTerms.append(token);
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

  private List<String> tokenise(String raw) {
    List<String> tokens = new ArrayList<>();
    Matcher m = TOKEN_PATTERN.matcher(raw);
    while (m.find()) tokens.add(m.group());
    return tokens;
  }
}
