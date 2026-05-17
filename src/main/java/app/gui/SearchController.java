package app.gui;

import app.config.IndexConfig;
import app.db.FileRepository;
import app.gui.widget.GalleryWidget;
import app.gui.widget.LogAnalyzerWidget;
import app.gui.widget.WidgetFactory;
import app.indexer.IndexerFactory;
import app.model.IndexReport;
import app.model.SearchResult;
import app.search.RankingStrategy;
import app.search.SearchEngine;
import app.search.SearchHistoryService;
import app.util.FileTypes;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
  @FXML private ComboBox<String> colorFilter;
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
  @FXML private Label totalSearchesLabel;
  @FXML private Label totalFilesLabel;
  @FXML private Label avgPathScoreLabel;
  @FXML private ListView<String> topQueriesList;
  @FXML private ListView<String> topExtensionsList;
  @FXML private ListView<String> recentSearchesList;
  @FXML private StackPane imagePane;
  @FXML private ImageView imageView;
  @FXML private ScrollPane fullFileScroll;
  @FXML private HBox widgetStrip;

  private WidgetFactory widgetFactory;
  private SearchViewModel searchVM;
  private IndexViewModel indexVM;
  private PauseTransition liveSearchDelay;
  private SearchHistoryService historyService;
  private FileRepository repository;
  private final ContextMenu suggestionsPopup = new ContextMenu();

  public void init(IndexerFactory factory, SearchEngine engine, FileRepository repository) {
    this.repository = repository;
    historyService = new SearchHistoryService(repository);
    engine.addObserver(historyService);

    searchVM = new SearchViewModel(engine, repository, historyService);
    indexVM = new IndexViewModel(factory, repository, new app.processor.ContentExtractor());

    indexVM.setOnIndexComplete(
        () -> {
          searchVM.refreshExtensions();
          updateReportBox(indexVM.reportProperty().get());
          refreshAnalytics();
        });

    bindUI();
    setupWidgets();
    searchVM.setOnResultsReady(this::updateWidgetStrip);
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
    refreshAnalytics();
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

    reportFormatChoice.setItems(FXCollections.observableArrayList("TEXT", "JSON"));
    reportFormatChoice.setValue("TEXT");
    exportReportButton.disableProperty().bind(indexVM.reportProperty().isNull());

    sortChoice.setItems(
        FXCollections.observableArrayList(
            RankingStrategy.ALL.stream().map(RankingStrategy::label).toList()));
    sortChoice.setValue(RankingStrategy.RELEVANCE.label());
    sortChoice
        .valueProperty()
        .addListener(
            (obs, old, val) -> {
              RankingStrategy selected =
                  RankingStrategy.ALL.stream()
                      .filter(s -> s.label().equals(val))
                      .findFirst()
                      .orElse(RankingStrategy.RELEVANCE);
              searchVM.setStrategy(selected);
              triggerSearch();
            });

    imageView.fitWidthProperty().bind(tabPane.widthProperty().subtract(32));
    setupColorFilter();
  }

  private void setupColorFilter() {
    List<String> colors =
        List.of(
            "any color",
            "red",
            "orange",
            "yellow",
            "green",
            "cyan",
            "blue",
            "purple",
            "pink",
            "white",
            "black",
            "gray");

    colorFilter.setItems(FXCollections.observableArrayList(colors));
    colorFilter.setValue("any color");

    colorFilter.setCellFactory(
        lv ->
            new ListCell<>() {
              @Override
              protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setGraphic(null);
                  setText(null);
                  return;
                }
                if (item.equals("any color")) {
                  setGraphic(null);
                  setText("any color");
                  return;
                }
                Rectangle swatch = new Rectangle(12, 12);
                swatch.setArcWidth(3);
                swatch.setArcHeight(3);
                swatch.setFill(Color.web(hexForColor(item)));
                HBox box = new HBox(6, swatch, new Label(item));
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
              }
            });

    colorFilter.setButtonCell(
        new ListCell<>() {
          @Override
          protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null || item.equals("any color")) {
              setGraphic(null);
              setText("color");
              return;
            }
            Rectangle swatch = new Rectangle(10, 10);
            swatch.setArcWidth(2);
            swatch.setArcHeight(2);
            swatch.setFill(Color.web(hexForColor(item)));
            HBox box = new HBox(5, swatch, new Label(item));
            box.setAlignment(Pos.CENTER_LEFT);
            setGraphic(box);
            setText(null);
          }
        });

    colorFilter.valueProperty().addListener((obs, old, val) -> triggerSearch());
  }

  private String hexForColor(String name) {
    return switch (name.toLowerCase()) {
      case "red" -> "#ff5555";
      case "orange" -> "#ff9966";
      case "yellow" -> "#f1fa8c";
      case "green" -> "#a6e3a1";
      case "cyan" -> "#89dceb";
      case "blue" -> "#89b4fa";
      case "purple" -> "#cba6f7";
      case "pink" -> "#f38ba8";
      case "white" -> "#f5f5f5";
      case "black" -> "#45475a";
      case "gray" -> "#7c88a6";
      default -> "#6b768f";
    };
  }

  private void setupLiveSearch() {
    liveSearchDelay = new PauseTransition(Duration.millis(350));
    liveSearchDelay.setOnFinished(e -> triggerSearch());
    searchField
        .textProperty()
        .addListener(
            (obs, old, val) -> {
              liveSearchDelay.playFromStart();
              updateSuggestions(val.trim());
            });
    extFilter.valueProperty().addListener((obs, old, val) -> triggerSearch());
    dirFilter.textProperty().addListener((obs, old, val) -> liveSearchDelay.playFromStart());
  }

  private void updateSuggestions(String prefix) {
    suggestionsPopup.hide();
    if (prefix.isBlank()) return;
    List<String> suggestions = historyService.suggest(prefix);
    if (suggestions.isEmpty()) return;
    suggestionsPopup.getItems().clear();
    for (String suggestion : suggestions) {
      MenuItem item = new MenuItem(suggestion);
      item.setOnAction(
          e -> {
            searchField.setText(suggestion);
            searchField.positionCaret(suggestion.length());
            suggestionsPopup.hide();
            triggerSearch();
          });
      suggestionsPopup.getItems().add(item);
    }
    suggestionsPopup.show(searchField, Side.BOTTOM, 0, 0);
  }

  private void triggerSearch() {
    String query = searchField.getText().trim();
    String ext = extFilter.getValue();
    String dir = dirFilter.getText().trim();
    String color = colorFilter.getValue();

    boolean noQuery = query.isBlank();
    boolean noExt = ext == null || ext.isBlank();
    boolean noDir = dir.isBlank();
    boolean noColor = color == null || color.equals("any color");

    if (noQuery && noExt && noDir && noColor) {
      searchVM.getResults().clear();
      searchVM.resultCountProperty().set("");
      return;
    }

    searchVM.search(query, noExt ? "" : ext.trim(), dir, noColor ? null : color);

    if (!searchVM.getResults().isEmpty()) {
      resultsList.getSelectionModel().selectFirst();
    }
    refreshAnalytics();
  }

  private void refreshAnalytics() {
    totalSearchesLabel.setText(String.valueOf(historyService.totalSearches()));

    List<String> top =
        historyService.topQueries(10).stream().map(e -> e.getKey() + "  x" + e.getValue()).toList();
    topQueriesList.setItems(FXCollections.observableArrayList(top));

    List<String> recent =
        historyService.recentHistory(20).stream()
            .map(e -> e.timestamp().toString().substring(11, 19) + "  " + e.query())
            .toList();
    recentSearchesList.setItems(FXCollections.observableArrayList(recent));

    Thread.ofVirtual()
        .start(
            () -> {
              long fileCount = repository.countFiles();
              double avgScore = repository.averagePathScore();
              List<String> exts =
                  repository.topExtensions(8).stream()
                      .map(e -> "." + e.getKey() + "  " + e.getValue())
                      .toList();
              javafx.application.Platform.runLater(
                  () -> {
                    totalFilesLabel.setText(String.valueOf(fileCount));
                    avgPathScoreLabel.setText(String.format("%.2f", avgScore));
                    topExtensionsList.setItems(FXCollections.observableArrayList(exts));
                  });
            });
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
    colorFilter.setValue("any color");
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
            : "(no preview - binary or unreadable file)";
    String query = extractTerms(searchField.getText().trim());
    previewFlow.getChildren().setAll(TextHighlighter.highlight(content, query));
    tabPane.getSelectionModel().select(0);
    loadFullFile(result, query);
  }

  private void loadFullFile(SearchResult result, String query) {
    fullFileScroll.setVisible(true);
    fullFileScroll.setManaged(true);
    imagePane.setVisible(false);
    imagePane.setManaged(false);
    imageView.setImage(null);

    String ext = result.extension();

    if (FileTypes.isImage(ext)) {
      if (result.sizeBytes() > MAX_PREVIEW_BYTES) {
        showFullFileMessage(
            String.format(
                "(image too large to preview: %.1f MB)", result.sizeBytes() / (1024.0 * 1024)));
        return;
      }
      final String path = result.path();
      final SearchResult token = result;
      Thread.ofVirtual()
          .start(
              () -> {
                try {
                  Image img = new Image(java.nio.file.Path.of(path).toUri().toString(), true);
                  javafx.application.Platform.runLater(
                      () -> {
                        if (resultsList.getSelectionModel().getSelectedItem() != token) return;
                        imageView.setImage(img);
                        fullFileScroll.setVisible(false);
                        fullFileScroll.setManaged(false);
                        imagePane.setVisible(true);
                        imagePane.setManaged(true);
                      });
                } catch (Exception e) {
                  javafx.application.Platform.runLater(
                      () -> showFullFileMessage("(could not load image: " + e.getMessage() + ")"));
                }
              });
      return;
    }

    String c = repository.getFullContent(result.path());
    if (c == null || c.isBlank()) {
      showFullFileMessage("(binary or non-text file - no readable content extracted)");
      return;
    }
    final String fc = c;
    final SearchResult token = result;
    Thread.ofVirtual()
        .start(
            () ->
                javafx.application.Platform.runLater(
                    () -> {
                      if (resultsList.getSelectionModel().getSelectedItem() != token) return;
                      fullFileFlow.getChildren().setAll(TextHighlighter.highlight(fc, query));
                    }));
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
      if (lower.startsWith("ext:")
          || lower.startsWith("path:")
          || lower.startsWith("content:")
          || lower.startsWith("name:")
          || lower.startsWith("color:")) continue;
      if (lower.equals("and") || lower.equals("or") || lower.equals("not")) continue;
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

  private void setupWidgets() {
    widgetFactory = new WidgetFactory();

    widgetFactory.register(
        new GalleryWidget(
            () -> {
              extFilter.setValue("jpg");
              colorFilter.setValue("any color");
              triggerSearch();
            }));

    widgetFactory.register(
        new LogAnalyzerWidget(
            () -> {
              extFilter.setValue(null);
              colorFilter.setValue("any color");
              String current = searchField.getText().trim();
              if (!current.contains("ext:log")) {
                searchField.setText(
                    (current.isBlank() ? "" : current + " ") + "ext:log ext:txt ext:err ext:out");
              }
              triggerSearch();
            }));
  }

  private void updateWidgetStrip(List<SearchResult> results) {
    List<app.gui.widget.ContextWidget> active = widgetFactory.evaluate(results);
    widgetStrip.getChildren().clear();

    if (active.isEmpty()) {
      widgetStrip.setVisible(false);
      widgetStrip.setManaged(false);
      return;
    }

    Label hint = new Label("Suggestions:");
    hint.getStyleClass().add("widget-label");
    widgetStrip.getChildren().add(hint);

    for (app.gui.widget.ContextWidget widget : active) {
      Button btn = new Button(widget.actionLabel());
      btn.getStyleClass().add("widget-btn");
      Tooltip.install(btn, new Tooltip(widget.description()));
      btn.setOnAction(
          e -> {
            widget.action().run();
            widgetStrip.setVisible(false);
            widgetStrip.setManaged(false);
          });
      widgetStrip.getChildren().add(btn);
    }

    widgetStrip.setVisible(true);
    widgetStrip.setManaged(true);
  }

  public boolean isIndexing() {
    return indexVM.isIndexing();
  }

  public void stopWatcher() {
    indexVM.stopWatcher();
  }
}
