package app.gui.widget;

import app.model.SearchResult;
import app.util.FileTypes;
import java.util.List;

public class GalleryWidget implements ContextWidget {

  private static final double THRESHOLD = 0.4;

  private boolean active = false;
  private Runnable onActivate;

  public GalleryWidget(Runnable onActivate) {
    this.onActivate = onActivate;
  }

  @Override
  public void onResults(List<SearchResult> results) {
    if (results.isEmpty()) {
      active = false;
      return;
    }
    long imageCount =
        results.stream()
            .filter(r -> r.extension() != null && FileTypes.isImage(r.extension()))
            .count();
    active = (double) imageCount / results.size() >= THRESHOLD;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public String label() {
    return "Gallery View";
  }

  @Override
  public String description() {
    return "Results contain many images";
  }

  @Override
  public String actionLabel() {
    return "View as Gallery";
  }

  @Override
  public Runnable action() {
    return onActivate;
  }
}
