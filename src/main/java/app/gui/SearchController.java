package app.gui;

import app.config.IndexConfig;
import app.db.FileRepository;
import app.indexer.IndexerFactory;
import app.model.IndexReport;
import app.model.SearchResult;
import app.search.SearchEngine;
import app.search.SortOrder;
import app.util.FileTypes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SearchController {

  private static final long MAX_PREVIEW_BYTES = 2_000_000L;

  @FXML private TextField searchField;
  @FXML private TextField pathField;
  @FXML private TextField dirFilter;
  @FXML private ComboBox<String> extFilter;
  @FXML private ListView<SearchResult> resultsList;
  @FXML private TextFlow previewFlow;
  @FXML private TextFlow fullFileFlow;
  @FXML private Label statusLabel;
  @FXML private Label resultCountLabel;
  @FXML private Label previewFileLabel;
  @FXML private Label filePathLabel;
  @FXML private Label fileExtLabel;
  @FXML private Label reportErrors;
  @FXML private Label reportTime;
  @FXML private ProgressBar progressBar;
  @FXML private HBox reportBox;
  @FXML private TabPane tabPane;
  @FXML private Button loadMoreButton;
  @FXML private ChoiceBox<String> sortChoice;
  @FXML private TextArea ignoredExtsField;
  @FXML private TextArea ignoredDirsField;
  @FXML private VBox settingsBox;
  @FXML private Button settingsToggle;
  @FXML private ChoiceBox<String> reportFormatChoice;
  @FXML private Button exportReportButton;
  @FXML private Label reportUpToDate;
  @FXML private Label reportFiltered;
  @FXML private Label reportTotal;
  @FXML private Label reportNew;
  @FXML private Label reportUpdated;

  private SearchViewModel searchVM;
  private IndexViewModel indexVM;
  private PauseTransition liveSearchDelay;

  public void init(IndexerFactory factory, SearchEngine engine, FileRepository repository) {
    searchVM = new SearchViewModel(engine, repository);
    indexVM = new IndexViewModel(factory);

    indexVM.setOnIndexComplete(
        () -> {
          searchVM.refreshExtensions();
          updateReportBox(indexVM.reportProperty().get());
        });

    bindUI();

    ignoredExtsField.setText(
        IndexConfig.DEFAULT_IGNORED_EXTS.stream()
            .sorted()
            .collect(java.util.stream.Collectors.joining("\n")));
    ignoredDirsField.setText(
        IndexConfig.DEFAULT_IGNORED_DIR_NAMES.stream()
            .sorted()
            .collect(java.util.stream.Collectors.joining("\n")));

    setupLiveSearch();
    searchVM.refreshExtensions();
  }

  private void bindUI() {
    resultsList.setFixedCellSize(90);
    resultsList.setCellFactory(new ResultCellFactory());
    resultsList.setItems(searchVM.getResults());
    resultsList
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, old, selected) -> onResultSelected(selected));

    extFilter.setItems(searchVM.getAvailableExtensions());
    resultCountLabel.textProperty().bind(searchVM.resultCountProperty());
    statusLabel.textProperty().bind(indexVM.statusProperty());

    progressBar.visibleProperty().bind(indexVM.indexingProperty());
    progressBar.managedProperty().bind(indexVM.indexingProperty());
    progressBar
        .progressProperty()
        .bind(
            javafx.beans.binding.Bindings.when(indexVM.indexingProperty())
                .then(-1.0)
                .otherwise(0.0));

    reportBox.visibleProperty().bind(indexVM.reportProperty().isNotNull());
    reportBox.managedProperty().bind(indexVM.reportProperty().isNotNull());
    loadMoreButton.visibleProperty().bind(searchVM.hasMoreProperty());
    loadMoreButton.managedProperty().bind(searchVM.hasMoreProperty());

    reportFormatChoice.setItems(
        javafx.collections.FXCollections.observableArrayList("TEXT", "JSON"));
    reportFormatChoice.setValue("TEXT");
    exportReportButton.disableProperty().bind(indexVM.reportProperty().isNull());

    sortChoice.setItems(
        javafx.collections.FXCollections.observableArrayList("Relevance", "Date", "Size"));
    sortChoice.setValue("Relevance");
    sortChoice
        .valueProperty()
        .addListener(
            (obs, old, val) -> {
              searchVM.setSortOrder(
                  switch (val) {
                    case "Date" -> SortOrder.DATE;
                    case "Size" -> SortOrder.SIZE;
                    default -> SortOrder.RELEVANCE;
                  });
              triggerSearch();
            });
  }

  private void setupLiveSearch() {
    liveSearchDelay = new PauseTransition(Duration.millis(350));
    liveSearchDelay.setOnFinished(e -> triggerSearch());
    searchField.textProperty().addListener((obs, old, val) -> liveSearchDelay.playFromStart());
    extFilter.valueProperty().addListener((obs, old, val) -> triggerSearch());
    dirFilter.textProperty().addListener((obs, old, val) -> liveSearchDelay.playFromStart());
  }

  private void triggerSearch() {
    String query = searchField.getText().trim();
    if (query.isBlank()) return;
    searchVM.search(query, extFilter.getValue(), dirFilter.getText().trim());
    if (!searchVM.getResults().isEmpty()) resultsList.getSelectionModel().selectFirst();
  }

  @FXML
  private void onSearch() {
    liveSearchDelay.stop();
    triggerSearch();
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
  private void onIndex() {
    Set<String> ignoredExts =
        Arrays.stream(ignoredExtsField.getText().split("[,\n]"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(java.util.stream.Collectors.toSet());
    Set<String> ignoredDirs =
        Arrays.stream(ignoredDirsField.getText().split("\n"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(name -> name.startsWith(".") ? "^\\" + name + ".*" : ".*" + name + ".*")
            .collect(java.util.stream.Collectors.toSet());
    indexVM.index(pathField.getText().trim(), ignoredDirs, ignoredExts);
  }

  @FXML
  private void onToggleSettings() {
    boolean visible = !settingsBox.isVisible();
    settingsBox.setVisible(visible);
    settingsBox.setManaged(visible);
    settingsToggle.setText(visible ? "▾ Ignore rules" : "▸ Ignore rules");
  }

  @FXML
  private void onClearFilters() {
    extFilter.setValue(null);
    dirFilter.clear();
    triggerSearch();
  }

  @FXML
  private void onLoadMore() {
    searchVM.loadMore();
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

    previewFlow.getChildren().setAll(TextHighlighter.highlight(content, query));
    tabPane.getSelectionModel().select(0);
    loadFullFile(result, query);
  }

  private void loadFullFile(SearchResult result, String query) {
    String ext = result.extension();
    if (ext == null || ext.isBlank() || !FileTypes.isText(ext)) {
      showFullFileMessage("(binary or non-text file — see preview above)");
      return;
    }
    if (result.sizeBytes() > MAX_PREVIEW_BYTES) {
      showFullFileMessage(
          String.format(
              "(file too large to preview: %.1f MB — limit is %.1f MB)",
              result.sizeBytes() / (1024.0 * 1024), MAX_PREVIEW_BYTES / (1024.0 * 1024)));
      return;
    }

    showFullFileMessage("Loading…");
    final String path = result.path();
    final SearchResult token = result;
    Thread.ofVirtual()
        .start(
            () -> {
              String content;
              try {
                content = Files.readString(Path.of(path));
              } catch (IOException e) {
                content = "(could not read file: " + e.getMessage() + ")";
              }
              final String finalContent = content;
              javafx.application.Platform.runLater(
                  () -> {
                    if (resultsList.getSelectionModel().getSelectedItem() == token)
                      fullFileFlow
                          .getChildren()
                          .setAll(TextHighlighter.highlight(finalContent, query));
                  });
            });
  }

  private void showFullFileMessage(String message) {
    Text t = new Text(message);
    t.setFill(Color.web("#7f849c"));
    t.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
    fullFileFlow.getChildren().setAll(t);
  }

  private void updateReportBox(IndexReport report) {
    if (report == null) return;
    reportTotal.setText(String.valueOf(report.filesTotal()));
    reportNew.setText(String.valueOf(report.filesNew()));
    reportUpdated.setText(String.valueOf(report.filesUpdated()));
    reportUpToDate.setText(String.valueOf(report.filesUpToDate()));
    reportFiltered.setText(String.valueOf(report.filesFiltered()));
    reportErrors.setText(String.valueOf(report.errors()));
    reportTime.setText(String.format("%.2fs", report.elapsedSeconds()));
  }

  private String extractTerms(String raw) {
    if (raw == null || raw.isBlank()) return "";
    StringBuilder sb = new StringBuilder();
    for (String part : raw.trim().split("\\s+")) {
      String lower = part.toLowerCase();
      if (lower.startsWith("ext:") || lower.startsWith("path:") || lower.startsWith("content:"))
        continue;
      if (!sb.isEmpty()) sb.append(" ");
      sb.append(part);
    }
    return sb.toString();
  }

  @FXML
  private void onExportReport() {
    IndexReport report = indexVM.reportProperty().get();
    if (report == null) return;
    String format = reportFormatChoice.getValue();
    String content = format.equals("JSON") ? toJson(report) : toText(report);
    String ext = format.equals("JSON") ? ".json" : ".txt";

    javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
    chooser.setTitle("Save Report");
    chooser.setInitialFileName("index_report" + ext);
    chooser
        .getExtensionFilters()
        .add(new javafx.stage.FileChooser.ExtensionFilter(format + " file", "*" + ext));

    java.io.File file = chooser.showSaveDialog(pathField.getScene().getWindow());
    if (file == null) return;
    try {
      java.nio.file.Files.writeString(file.toPath(), content);
      indexVM.setStatus("Report saved to " + file.getName());
    } catch (java.io.IOException e) {
      indexVM.setStatus("Failed to save report: " + e.getMessage());
    }
  }

  private String toText(IndexReport report) {
    return String.format(
        """
        ========================================
        Root        : %s
        Total       : %d
          New       : %d
          Updated   : %d
          Up to date: %d
          Filtered  : %d
        Dirs        : %d
        Errors      : %d
        Time        : %.2fs
        ========================================
        """,
        report.rootDir(),
        report.filesTotal(),
        report.filesNew(),
        report.filesUpdated(),
        report.filesUpToDate(),
        report.filesFiltered(),
        report.directoriesVisited(),
        report.errors(),
        report.elapsedSeconds());
  }

  private String toJson(IndexReport report) {
    return String.format(
        """
        {
          "rootDir": "%s",
          "filesTotal": %d,
          "filesNew": %d,
          "filesUpdated": %d,
          "filesUpToDate": %d,
          "filesFiltered": %d,
          "directoriesVisited": %d,
          "errors": %d,
          "elapsedSeconds": %.2f
        }
        """,
        report.rootDir(),
        report.filesTotal(),
        report.filesNew(),
        report.filesUpdated(),
        report.filesUpToDate(),
        report.filesFiltered(),
        report.directoriesVisited(),
        report.errors(),
        report.elapsedSeconds());
  }

  public boolean isIndexing() {
    return indexVM.isIndexing();
  }
}
