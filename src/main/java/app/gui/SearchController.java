package app.gui;

import app.indexer.FileIndexer;
import app.model.SearchResult;
import app.search.SearchEngine;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class SearchController {

    @FXML private TextField  searchField;
    @FXML private TextField  pathField;
    @FXML private ListView<SearchResult> resultsList;
    @FXML private TextArea   previewArea;
    @FXML private Label      statusLabel;

    private SearchEngine engine;
    private FileIndexer  indexer;

    public void init(FileIndexer indexer, SearchEngine engine) {
        this.indexer = indexer;
        this.engine  = engine;

        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SearchResult r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null);
                } else {
                    setText(r.name() + "\n" + r.path());
                }
            }
        });

        resultsList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> onResultSelected(selected)
        );
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        List<SearchResult> results = engine.search(query);
        resultsList.getItems().setAll(results);
        statusLabel.setText("Found " + results.size() + " result(s) for: " + query);
        previewArea.clear();
    }

    @FXML
    private void onIndex() {
        String path = pathField.getText().trim();
        if (!path.isEmpty()) {
            statusLabel.setText("Indexing...");
            indexer.index();
            statusLabel.setText("Indexing complete.");
        }
    }

    private void onResultSelected(SearchResult result) {
        if (result == null) return;
        previewArea.setText(
                "File:    " + result.name() + "\n" +
                        "Path:    " + result.path() + "\n" +
                        "Ext:     " + result.extension() + "\n\n" +
                        (result.snippet() != null ? result.snippet() : "(no preview)")
        );
    }
}