package app.gui.widget;

import app.model.SearchResult;
import java.util.ArrayList;
import java.util.List;

public class WidgetFactory {

  private final List<ContextWidget> widgets = new ArrayList<>();

  public void register(ContextWidget widget) {
    widgets.add(widget);
  }

  public List<ContextWidget> evaluate(List<SearchResult> results) {
    widgets.forEach(w -> w.onResults(results));
    return widgets.stream().filter(ContextWidget::isActive).toList();
  }
}
