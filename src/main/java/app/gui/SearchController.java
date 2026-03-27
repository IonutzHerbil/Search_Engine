package app.gui;

import app.indexer.FileIndexer;
import app.indexer.IndexerFactory;
import app.model.IndexReport;
import app.model.SearchResult;
import app.search.SearchEngine;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SearchController {

    @FXML private TextField   searchField;
    @FXML private TextField   pathField;
    @FXML private ListView<SearchResult> resultsList;
    @FXML private TextFlow    previewFlow;
    @FXML private Label       statusLabel;
    @FXML private Label       resultCountLabel;
    @FXML private Label       previewFileLabel;
    @FXML private Label       filePathLabel;
    @FXML private Label       fileExtLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button      viewFullButton;

    private SearchEngine    engine;
    private IndexerFactory  factory;
    private SearchResult    selectedResult;
    private PauseTransition liveSearchDelay;

    public void init(IndexerFactory factory, SearchEngine engine) {
        this.factory = factory;
        this.engine  = engine;

        progressIndicator.setVisible(false);

        setupResultsList();
        setupLiveSearch();
    }

    private void setupResultsList() {
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SearchResult r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }

                VBox box = new VBox(3);
                box.setPadding(new Insets(8, 12, 8, 12));

                Label name = new Label(r.name());
                name.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -color-fg-default;");

                Label path = new Label(shortenPath(r.path()));
                path.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-fg-muted; -fx-font-family: 'Consolas', monospace;");
                path.setWrapText(true);

                box.getChildren().addAll(name, path);

                if (r.snippet() != null && !r.snippet().isBlank()) {
                    Label snippet = new Label(r.snippet().lines().findFirst().orElse(""));
                    snippet.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-fg-subtle; -fx-font-family: 'Consolas', monospace;");
                    box.getChildren().add(snippet);
                }

                setText(null);
                setGraphic(box);
            }
        });

        resultsList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> onResultSelected(selected)
        );
    }

    private void setupLiveSearch() {
        liveSearchDelay = new PauseTransition(Duration.millis(350));
        liveSearchDelay.setOnFinished(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) performSearch(query);
        });
        searchField.textProperty().addListener((obs, old, val) -> liveSearchDelay.playFromStart());
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

        Task<IndexReport> task = new Task<>() {
            @Override
            protected IndexReport call() {
                return indexer.index();
            }
        };

        task.setOnRunning(e -> Platform.runLater(() -> {
            progressIndicator.setVisible(true);
            statusLabel.setText("Indexing " + path + "...");
        }));

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            IndexReport report = task.getValue();
            progressIndicator.setVisible(false);
            statusLabel.setText("Done. " + report.filesFound() + " indexed, " + report.skipped() + " skipped.");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            statusLabel.setText("Failed: " + task.getException().getMessage());
        }));

        new Thread(task).start();
    }

    private void onResultSelected(SearchResult result) {
        if (result == null) return;
        selectedResult = result;

        previewFileLabel.setText(result.name());
        filePathLabel.setText(result.path());
        fileExtLabel.setText(result.extension() != null ? "." + result.extension() : "");
        viewFullButton.setDisable(false);

        String content = result.snippet() != null ? result.snippet() : "(no preview available)";
        String query   = extractTerms(searchField.getText().trim());

        previewFlow.getChildren().setAll(buildHighlightedText(content, query));
    }

    @FXML
    private void onViewFullFile() {
        if (selectedResult == null) return;

        String content;
        try {
            content = Files.readString(Path.of(selectedResult.path()));
        } catch (IOException e) {
            content = "Could not read file: " + e.getMessage();
        }

        TextArea area = new TextArea(content);
        area.setEditable(false);
        area.setWrapText(false);
        area.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");

        Stage stage = new Stage();
        stage.setTitle(selectedResult.name() + "  —  " + selectedResult.path());
        stage.setScene(new Scene(area, 900, 650));
        stage.show();
    }

    private List<Text> buildHighlightedText(String content, String query) {
        java.util.List<Text> nodes = new java.util.ArrayList<>();

        if (query == null || query.isBlank()) {
            nodes.add(styledText(content, false));
            return nodes;
        }

        String lower = content.toLowerCase();
        String term  = query.toLowerCase();
        int    start = 0;

        while (true) {
            int idx = lower.indexOf(term, start);
            if (idx == -1) {
                nodes.add(styledText(content.substring(start), false));
                break;
            }
            if (idx > start) {
                nodes.add(styledText(content.substring(start, idx), false));
            }
            nodes.add(styledText(content.substring(idx, idx + term.length()), true));
            start = idx + term.length();
        }

        return nodes;
    }

    private Text styledText(String content, boolean highlight) {
        Text t = new Text(content);
        if (highlight) {
            t.setFill(Color.web("#a6e3a1"));
            t.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            t.setFill(Color.web("#cdd6f4"));
            t.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        }
        return t;
    }

    private String extractTerms(String raw) {
        if (raw == null || raw.isBlank()) return "";
        StringBuilder terms = new StringBuilder();
        for (String part : raw.trim().split("\\s+")) {
            if (!part.toLowerCase().startsWith("ext:")) {
                if (!terms.isEmpty()) terms.append(" ");
                terms.append(part);
            }
        }
        return terms.toString();
    }

    private String shortenPath(String path) {
        if (path == null) return "";
        if (path.length() <= 55) return path;
        return "..." + path.substring(path.length() - 52);
    }
}
