package app.gui.widget;

import app.model.SearchResult;
import java.util.List;
import java.util.Set;

public class LogAnalyzerWidget implements ContextWidget {

  private static final double THRESHOLD = 0.4;
  private static final Set<String> LOG_EXTENSIONS = Set.of("log", "txt", "out", "err");

  private boolean active = false;
  private Runnable onActivate;

  public LogAnalyzerWidget(Runnable onActivate) {
    this.onActivate = onActivate;
  }

  @Override
  public void onResults(List<SearchResult> results) {
    if (results.isEmpty()) {
      active = false;
      return;
    }
    long logCount =
        results.stream()
            .filter(
                r -> r.extension() != null && LOG_EXTENSIONS.contains(r.extension().toLowerCase()))
            .count();
    active = (double) logCount / results.size() >= THRESHOLD;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public String label() {
    return "Log Analyzer";
  }

  @Override
  public String description() {
    return "Results contain many log files";
  }

  @Override
  public String actionLabel() {
    return "Analyze Logs";
  }

  @Override
  public Runnable action() {
    return onActivate;
  }
}
