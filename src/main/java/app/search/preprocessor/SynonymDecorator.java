package app.search.preprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SynonymDecorator implements QueryPreProcessor {

  private final QueryPreProcessor delegate;
  private final Map<String, String> synonyms;

  public SynonymDecorator(QueryPreProcessor delegate) {
    this.delegate = delegate;
    this.synonyms = loadSynonyms();
  }

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("\"[^\"]+\"|AND|OR|NOT|\\w+:[^\\s]+|\\w+(-\\w+)+\\*?|\\w+\\*?|\\S+");

  @Override
  public String process(String raw) {
    String processed = delegate.process(raw);
    if (processed.isBlank() || synonyms.isEmpty()) return processed;

    Matcher m = TOKEN_PATTERN.matcher(processed);
    StringBuilder result = new StringBuilder();

    while (m.find()) {
      String token = m.group();
      if (!result.isEmpty()) result.append(" ");
      if (isQualifier(token) || isOperator(token) || isQuotedPhrase(token)) {
        result.append(token);
      } else {
        String bare = token.endsWith("*") ? token.substring(0, token.length() - 1) : token;
        String expanded = synonyms.get(bare.toLowerCase());
        if (expanded != null) {
          result.append("(").append(expanded).append(")");
        } else {
          result.append(token);
        }
      }
    }

    return result.toString().trim();
  }

  private Map<String, String> loadSynonyms() {
    Map<String, String> map = new HashMap<>();
    try (InputStream in =
        SynonymDecorator.class.getResourceAsStream("/app/search/synonyms.properties")) {
      if (in == null) {
        System.err.println("[SYNONYM] synonyms.properties not found");
        return map;
      }
      Properties props = new Properties();
      props.load(in);
      for (String key : props.stringPropertyNames()) {
        map.put(key.toLowerCase().trim(), props.getProperty(key).trim());
      }
    } catch (IOException e) {
      System.err.println("[SYNONYM] Failed to load synonyms: " + e.getMessage());
    }
    return map;
  }

  private boolean isQualifier(String t) {
    String l = t.toLowerCase();
    return l.startsWith("ext:")
        || l.startsWith("path:")
        || l.startsWith("name:")
        || l.startsWith("content:")
        || l.startsWith("color:");
  }

  private boolean isOperator(String t) {
    return t.equalsIgnoreCase("AND") || t.equalsIgnoreCase("OR") || t.equalsIgnoreCase("NOT");
  }

  private boolean isQuotedPhrase(String t) {
    return t.startsWith("\"") && t.endsWith("\"");
  }
}
