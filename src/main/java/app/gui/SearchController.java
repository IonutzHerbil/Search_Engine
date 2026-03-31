package app.gui;

import app.db.FileRepository;
import app.indexer.IndexerFactory;
import app.model.IndexReport;
import app.model.SearchResult;
import app.search.SearchEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import app.search.SortOrder;

public class SearchController {

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
  @FXML private Label reportIndexed;
  @FXML private Label reportSkipped;
  @FXML private Label reportDirs;
  @FXML private Label reportErrors;
  @FXML private Label reportTime;
  @FXML private ProgressBar progressBar;
  @FXML private HBox reportBox;
  @FXML private TabPane tabPane;
  @FXML private Button loadMoreButton;
  @FXML private ChoiceBox<String> sortChoice;

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

    sortChoice.setItems(javafx.collections.FXCollections.observableArrayList(
            "Relevance", "Date", "Size"));
    sortChoice.setValue("Relevance");
    sortChoice.valueProperty().addListener((obs, old, val) -> {
      searchVM.setSortOrder(switch (val) {
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
    indexVM.index(pathField.getText().trim());
  }

  @FXML
  private void onClearFilters() {
    extFilter.setValue(null);
    dirFilter.clear();
    triggerSearch();
  }

  @FXML
  private void onViewFullFile() {
    tabPane.getSelectionModel().select(1);
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
    loadFullFile(result.path(), query);
  }

  private void loadFullFile(String filePath, String query) {
    String content;
    try {
      content = Files.readString(Path.of(filePath));
    } catch (IOException e) {
      content = "(could not read file: " + e.getMessage() + ")";
    }
    fullFileFlow.getChildren().setAll(TextHighlighter.highlight(content, query));
  }

  private void updateReportBox(IndexReport report) {
    if (report == null) return;
    reportIndexed.setText(String.valueOf(report.filesFound()));
    reportSkipped.setText(String.valueOf(report.skipped()));
    reportDirs.setText(String.valueOf(report.directoriesVisited()));
    reportErrors.setText(String.valueOf(report.errors()));
    reportTime.setText(String.format("%.2fs", report.elapsedSeconds()));
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
}
