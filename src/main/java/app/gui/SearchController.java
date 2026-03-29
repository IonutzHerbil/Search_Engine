package app.gui;

import app.indexer.FileIndexer;
import app.indexer.IndexerFactory;
import app.model.IndexReport;
import app.model.SearchResult;
import app.search.SearchEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SearchController {

  @FXML private TextField searchField;
  @FXML private TextField pathField;
  @FXML private ListView<SearchResult> resultsList;
  @FXML private TextFlow previewFlow;
  @FXML private TextFlow fullFileFlow;
  @FXML private Label statusLabel;
  @FXML private Label resultCountLabel;
  @FXML private Label previewFileLabel;
  @FXML private Label filePathLabel;
  @FXML private Label fileExtLabel;
  @FXML private Label reportIndexed;
  @FXML private Label reportSkipped;
  @FXML private Label reportDirs;
  @FXML private Label reportErrors;
  @FXML private Label reportTime;
  @FXML private ProgressBar progressBar;
  @FXML private HBox reportBox;
  @FXML private TabPane tabPane;

  private SearchEngine engine;
  private IndexerFactory factory;
  private PauseTransition liveSearchDelay;
  private String currentFullFileContent;

  public void init(IndexerFactory factory, SearchEngine engine) {
    this.factory = factory;
    this.engine = engine;

    progressBar.setProgress(0);
    progressBar.setVisible(false);
    progressBar.setManaged(false);

    resultsList.setFixedCellSize(72);

    setupResultsList();
    setupLiveSearch();
  }

  private void setupResultsList() {
    resultsList.setCellFactory(
        lv ->
            new ListCell<>() {
              private final VBox box = new VBox(2);
              private final HBox topRow = new HBox(6);
              private final Label nameLabel = new Label();
              private final Label extLabel = new Label();
              private final Label pathLabel = new Label();
              private final Label snippetLabel = new Label();
              private final Region spacer = new Region();

              {
                box.setPadding(new Insets(8, 14, 8, 14));

                nameLabel.getStyleClass().add("cell-name");
                extLabel.getStyleClass().add("cell-ext");
                pathLabel.getStyleClass().add("cell-path");
                snippetLabel.getStyleClass().add("cell-snippet");

                HBox.setHgrow(spacer, Priority.ALWAYS);
                topRow.setAlignment(Pos.CENTER_LEFT);
                topRow.getChildren().addAll(nameLabel, spacer, extLabel);

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
            });

    resultsList
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, old, selected) -> onResultSelected(selected));
  }

  private void setupLiveSearch() {
    liveSearchDelay = new PauseTransition(Duration.millis(350));
    liveSearchDelay.setOnFinished(
        e -> {
          String query = searchField.getText().trim();
          if (!query.isEmpty()) performSearch(query);
        });
    searchField.textProperty().addListener((obs, old, val) -> liveSearchDelay.playFromStart());
  }

  @FXML
  private void onBrowse() {
    DirectoryChooser chooser = new DirectoryChooser();
    chooser.setTitle("Select Directory to Index");
    Stage stage = (Stage) pathField.getScene().getWindow();
    java.io.File dir = chooser.showDialog(stage);
    if (dir != null) pathField.setText(dir.getAbsolutePath());
  }

  @FXML
  private void onSearch() {
    liveSearchDelay.stop();
    String query = searchField.getText().trim();
    if (!query.isEmpty()) performSearch(query);
  }

  private void performSearch(String query) {
    List<SearchResult> results = engine.search(query);
    resultsList.getItems().setAll(results);
    resultCountLabel.setText(results.size() + " result" + (results.size() == 1 ? "" : "s"));
    if (!results.isEmpty()) resultsList.getSelectionModel().selectFirst();
  }

  @FXML
  private void onIndex() {
    String path = pathField.getText().trim();
    if (path.isEmpty()) {
      statusLabel.setText("Enter a directory path first.");
      return;
    }

    FileIndexer indexer = factory.create(path);

    reportBox.setVisible(false);
    reportBox.setManaged(false);
    progressBar.setVisible(true);
    progressBar.setManaged(true);
    progressBar.setProgress(-1);

    Task<IndexReport> task =
        new Task<>() {
          @Override
          protected IndexReport call() {
            return indexer.index(
                name -> Platform.runLater(() -> statusLabel.setText("Indexing: " + name)));
          }
        };

    task.setOnRunning(e -> Platform.runLater(() -> statusLabel.setText("Indexing…")));

    task.setOnSucceeded(
        e ->
            Platform.runLater(
                () -> {
                  IndexReport report = task.getValue();
                  progressBar.setVisible(false);
                  progressBar.setManaged(false);
                  statusLabel.setText("Done.");

                  reportIndexed.setText(String.valueOf(report.filesFound()));
                  reportSkipped.setText(String.valueOf(report.skipped()));
                  reportDirs.setText(String.valueOf(report.directoriesVisited()));
                  reportErrors.setText(String.valueOf(report.errors()));
                  reportTime.setText(String.format("%.2fs", report.elapsedSeconds()));

                  reportBox.setVisible(true);
                  reportBox.setManaged(true);
                }));

    task.setOnFailed(
        e ->
            Platform.runLater(
                () -> {
                  progressBar.setVisible(false);
                  progressBar.setManaged(false);
                  statusLabel.setText("Failed: " + task.getException().getMessage());
                }));

    new Thread(task).start();
  }

  private void onResultSelected(SearchResult result) {
    if (result == null) return;

    previewFileLabel.setText(result.name());
    filePathLabel.setText(result.path());
    fileExtLabel.setText(
        result.extension() != null && !result.extension().isBlank()
            ? "." + result.extension()
            : "");

    String content =
        result.snippet() != null && !result.snippet().isBlank()
            ? result.snippet()
            : "(no preview — binary or unreadable file)";
    String query = extractTerms(searchField.getText().trim());

    previewFlow.getChildren().setAll(buildHighlightedText(content, query));
    tabPane.getSelectionModel().select(0);
    loadFullFile(result.path(), query);
  }

  private void loadFullFile(String filePath, String query) {
    try {
      currentFullFileContent = Files.readString(Path.of(filePath));
    } catch (IOException e) {
      currentFullFileContent = "(could not read file: " + e.getMessage() + ")";
    }
    fullFileFlow.getChildren().setAll(buildHighlightedText(currentFullFileContent, query));
  }

  @FXML
  private void onViewFullFile() {
    tabPane.getSelectionModel().select(1);
  }

  private List<Node> buildHighlightedText(String content, String query) {
    List<Node> nodes = new ArrayList<>();

    if (query == null || query.isBlank()) {
      nodes.add(styledText(content, false));
      return nodes;
    }

    String lower = content.toLowerCase();
    String term = query.toLowerCase();
    int start = 0;

    while (true) {
      int idx = lower.indexOf(term, start);
      if (idx == -1) {
        if (start < content.length()) nodes.add(styledText(content.substring(start), false));
        break;
      }
      if (idx > start) nodes.add(styledText(content.substring(start, idx), false));
      nodes.add(styledText(content.substring(idx, idx + term.length()), true));
      start = idx + term.length();
    }

    return nodes;
  }

  private Text styledText(String content, boolean highlight) {
    Text t = new Text(content);
    if (highlight) {
      t.setFill(Color.web("#a6e3a1"));
      t.setStyle(
          "-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold;");
    } else {
      t.setFill(Color.web("#cdd6f4"));
      t.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
    }
    return t;
  }

  private String extractTerms(String raw) {
    if (raw == null || raw.isBlank()) return "";
    StringBuilder sb = new StringBuilder();
    for (String p : raw.trim().split("\\s+")) {
      if (!p.toLowerCase().startsWith("ext:") && !p.toLowerCase().startsWith("dir:")) {
        if (!sb.isEmpty()) sb.append(" ");
        sb.append(p);
      }
    }
    return sb.toString();
  }

  private String shortenPath(String path, int max) {
    if (path == null) return "";
    if (path.length() <= max) return path;
    return "..." + path.substring(path.length() - 52);
  }
}
