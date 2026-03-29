package app.cli;

import app.indexer.FileIndexer;
import app.indexer.IndexerFactory;
import app.model.IndexReport;
import app.model.SearchResult;
import app.search.SearchEngine;
import java.util.List;
import java.util.Scanner;

public class CLI {

  private final IndexerFactory factory;
  private final SearchEngine engine;
  private final Scanner scanner;

  public CLI(IndexerFactory factory, SearchEngine engine) {
    this.factory = factory;
    this.engine = engine;
    this.scanner = new Scanner(System.in);
  }

  public void run() {
    String dir = promptDirectory();
    String format = promptFormat();

    FileIndexer indexer = factory.create(dir);

    System.out.println("Indexing: " + indexer.getRootDirectory());
    System.out.println("----------------------------------------");
    int[] count = {0};
    IndexReport report = indexer.index(name -> System.out.printf("[%d] %s%n", ++count[0], name));
    printReport(report, format);
    runSearchLoop();
  }

  private String promptDirectory() {
    System.out.print("Directory to index (default: " + System.getProperty("user.home") + "): ");
    String input = scanner.nextLine().trim();
    return input.isEmpty() ? System.getProperty("user.home") : input;
  }

  private String promptFormat() {
    System.out.print("Report format [TEXT/JSON] (default TEXT): ");
    String input = scanner.nextLine().trim().toUpperCase();
    return input.equals("JSON") ? "JSON" : "TEXT";
  }

  private void printReport(IndexReport report, String format) {
    if (format.equals("JSON")) {
      printReportJson(report);
    } else {
      printReportText(report);
    }
  }

  private void printReportText(IndexReport report) {
    System.out.println("========================================");
    System.out.printf("Finished in %.2fs%n", report.elapsedSeconds());
    System.out.printf("Files Indexed : %d%n", report.filesFound());
    System.out.printf("Files Skipped : %d%n", report.skipped());
    System.out.printf("Dirs Visited  : %d%n", report.directoriesVisited());
    System.out.printf("Errors        : %d%n", report.errors());
  }

  private void printReportJson(IndexReport report) {
    System.out.printf(
        """
            {
              "rootDir": "%s",
              "filesFound": %d,
              "skipped": %d,
              "directoriesVisited": %d,
              "errors": %d,
              "elapsedSeconds": %.2f
            }
            """,
        report.rootDir(),
        report.filesFound(),
        report.skipped(),
        report.directoriesVisited(),
        report.errors(),
        report.elapsedSeconds());
  }

  private void runSearchLoop() {
    System.out.println("\nSearch ready. Type 'exit' or empty line to quit.");
    System.out.println("Tip: use ext:<type> to filter by extension, dir:<path> to filter by directory - e.g. 'main ext:java dir:C/Users'");

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
