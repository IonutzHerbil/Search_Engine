package app.model;

public class TraversalStats {

  private int filesFound = 0;
  private int directoriesVisited = 0;
  private int skipped = 0;
  private int errors = 0;
  private final long startTime = System.currentTimeMillis();

  public void recordFile() {
    filesFound++;
  }

  public void recordDirectory() {
    directoriesVisited++;
  }

  public void recordSkipped() {
    skipped++;
  }

  public void recordError() {
    errors++;
  }

  public IndexReport toReport(String rootDirectory) {
    double elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0;
    return new IndexReport(
        rootDirectory, filesFound, skipped, directoriesVisited, errors, elapsedTime);
  }
}
