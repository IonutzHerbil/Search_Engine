package app.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public final class TextHighlighter {

  private static final Pattern PHRASE_OR_TOKEN = Pattern.compile("\"([^\"]+)\"|\\S+");

  private TextHighlighter() {}

  public static List<Node> highlight(String content, String query) {
    Set<String> terms = extractTerms(query);
    if (terms.isEmpty()) return List.of(plain(content));
    return highlightAll(content, terms);
  }

  private static Set<String> extractTerms(String query) {
    if (query == null || query.isBlank()) return Set.of();
    Set<String> terms = new LinkedHashSet<>();
    Matcher m = PHRASE_OR_TOKEN.matcher(query);
    while (m.find()) {
      String token = m.group();
      if (token.startsWith("\"")) {
        String inner = m.group(1);
        for (String word : inner.split("\\s+")) {
          String t = word.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
          if (!t.isBlank()) terms.add(t);
        }
      } else {
        String lower = token.toLowerCase();
        if (lower.equals("and") || lower.equals("or") || lower.equals("not")) continue;
        if (lower.startsWith("ext:")
            || lower.startsWith("path:")
            || lower.startsWith("content:")
            || lower.startsWith("name:")) continue;
        String t = token.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
        if (!t.isBlank()) terms.add(t);
      }
    }
    return terms;
  }

  private static List<Node> highlightAll(String content, Set<String> terms) {
    List<Node> nodes = new ArrayList<>();
    String lower = content.toLowerCase();
    int start = 0;

    while (start < content.length()) {
      int bestIdx = Integer.MAX_VALUE;
      int bestLen = 0;

      for (String term : terms) {
        int idx = lower.indexOf(term, start);
        if (idx != -1 && idx < bestIdx) {
          bestIdx = idx;
          bestLen = term.length();
        }
      }

      if (bestIdx == Integer.MAX_VALUE) {
        nodes.add(plain(content.substring(start)));
        break;
      }
      if (bestIdx > start) nodes.add(plain(content.substring(start, bestIdx)));
      nodes.add(highlighted(content.substring(bestIdx, bestIdx + bestLen)));
      start = bestIdx + bestLen;
    }

    return nodes;
  }

  private static Text plain(String s) {
    Text t = new Text(s);
    t.setFill(Color.web("#cdd6f4"));
    t.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
    return t;
  }

  private static Text highlighted(String s) {
    Text t = new Text(s);
    t.setFill(Color.web("#a6e3a1"));
    t.setStyle(
        "-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold;");
    return t;
  }
}
