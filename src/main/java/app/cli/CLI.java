package app.cli;

import app.indexer.FileIndexer;
import app.model.IndexReport;
import app.model.SearchResult;
import app.search.SearchEngine;

import java.util.List;
import java.util.Scanner;

public class CLI {

    private final FileIndexer  indexer;
    private final SearchEngine engine;

    public CLI(FileIndexer indexer, SearchEngine engine) {
        this.indexer = indexer;
        this.engine  = engine;
    }

    public void run() {
        IndexReport report = indexer.index();
        printReport(report);
        runSearchLoop();
    }

    private void printReport(IndexReport report) {
        System.out.println("========================================");
        System.out.printf("Finished in %.2fs%n",  report.elapsedSeconds());
        System.out.printf("Files Indexed : %d%n", report.filesFound());
        System.out.printf("Files Skipped : %d%n", report.skipped());
        System.out.printf("Dirs Visited  : %d%n", report.directoriesVisited());
        System.out.printf("Errors        : %d%n", report.errors());
    }

    private void runSearchLoop() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nSearch ready. Type 'exit' or empty line to quit.");
        System.out.println("Tip: use ext:<type> to filter by extension — e.g. 'main ext:java'");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty() || input.equalsIgnoreCase("exit")) break;
            printResults(engine.search(input));
        }
    }

    private void printResults(List<SearchResult> results) {
        if (results.isEmpty()) {
            System.out.println("No results.");
            return;
        }
        System.out.printf("%nFound %d result(s):%n%n", results.size());
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            System.out.printf("[%d] %s%n", i + 1, r.name());
            System.out.printf("    %s%n", r.path());
            if (r.snippet() != null && !r.snippet().isBlank()) {
                System.out.printf("    %s%n", r.snippet());
            }
            System.out.println();
        }
    }
}