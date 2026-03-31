package app.gui;

import app.model.SearchResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class ResultCellFactory implements Callback<ListView<SearchResult>, ListCell<SearchResult>> {

  @Override
  public ListCell<SearchResult> call(ListView<SearchResult> lv) {
    return new ListCell<>() {
      private final VBox box = new VBox(2);
      private final HBox topRow = new HBox(6);
      private final Label rankLabel = new Label();
      private final Label nameLabel = new Label();
      private final Label extLabel = new Label();
      private final Label pathLabel = new Label();
      private final Label snippetLabel = new Label();
      private final Region spacer = new Region();

      {
        box.setPadding(new Insets(8, 14, 8, 14));

        rankLabel.getStyleClass().add("cell-rank");
        nameLabel.getStyleClass().add("cell-name");
        extLabel.getStyleClass().add("cell-ext");
        pathLabel.getStyleClass().add("cell-path");
        snippetLabel.getStyleClass().add("cell-snippet");

        Tooltip.install(
            rankLabel, new Tooltip("BM25 relevance score — higher means more relevant"));

        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(rankLabel, nameLabel, spacer, extLabel);

        pathLabel.setMaxWidth(Double.MAX_VALUE);
        snippetLabel.setMaxWidth(Double.MAX_VALUE);

        box.getChildren().addAll(topRow, pathLabel, snippetLabel);
        box.setMaxWidth(Double.MAX_VALUE);
      }

      @Override
      protected void updateItem(SearchResult r, boolean empty) {
        super.updateItem(r, empty);
        if (empty || r == null) {
          setGraphic(null);
          return;
        }

        rankLabel.setText(String.format("%.2f", Math.abs(r.score())));
        nameLabel.setText(r.name());

        String ext = r.extension();
        extLabel.setText(ext != null && !ext.isBlank() ? ext : "—");
        pathLabel.setText(shortenPath(r.path(), 60));

        if (r.snippet() != null && !r.snippet().isBlank()) {
          String line = r.snippet().lines().findFirst().orElse("").trim();
          snippetLabel.setText(line.length() > 80 ? line.substring(0, 80) + "…" : line);
          snippetLabel.setVisible(true);
          snippetLabel.setManaged(true);
        } else {
          snippetLabel.setText("");
          snippetLabel.setVisible(false);
          snippetLabel.setManaged(false);
        }

        setGraphic(box);
      }

      private String shortenPath(String path, int max) {
        if (path == null) return "";
        if (path.length() <= max) return path;
        return "..." + path.substring(path.length() - (max - 1));
      }
    };
  }
}
