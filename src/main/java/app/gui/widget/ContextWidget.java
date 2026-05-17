package app.gui.widget;

import app.model.SearchResult;
import java.util.List;

public interface ContextWidget {
  void onResults(List<SearchResult> results);

  boolean isActive();

  String label();

  String description();

  String actionLabel();

  Runnable action();
}
