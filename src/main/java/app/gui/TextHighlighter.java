package app.gui;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public final class TextHighlighter {

  private TextHighlighter() {}

  public static List<Node> highlight(String content, String query) {
    List<Node> nodes = new ArrayList<>();

    if (query == null || query.isBlank()) {
      nodes.add(plain(content));
      return nodes;
    }

    String lower = content.toLowerCase();
    String term = query.toLowerCase();
    int start = 0;

    while (true) {
      int idx = lower.indexOf(term, start);
      if (idx == -1) {
        if (start < content.length()) nodes.add(plain(content.substring(start)));
        break;
      }
      if (idx > start) nodes.add(plain(content.substring(start, idx)));
      nodes.add(highlighted(content.substring(idx, idx + term.length())));
      start = idx + term.length();
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
        "-fx-font-family: 'Consolas', monospace;"
            + " -fx-font-size: 12px;"
            + " -fx-font-weight: bold;");
    return t;
  }
}
