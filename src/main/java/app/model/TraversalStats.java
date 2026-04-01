package app.model;

public class TraversalStats {

  private int filesNew = 0;
  private int filesUpdated = 0;
  private int filesUpToDate = 0;
  private int filesFiltered = 0;
  private int directoriesVisited = 0;
  private int errors = 0;
  private final long startTime = System.currentTimeMillis();

  public void recordNewFile()     { filesNew++; }
  public void recordUpdatedFile() { filesUpdated++; }
  public void recordUpToDate()    { filesUpToDate++; }
  public void recordFiltered()    { filesFiltered++; }
  public void recordDirectory()   { directoriesVisited++; }
  public void recordError()       { errors++; }

  public IndexReport toReport(String rootDirectory) {
    double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
    int total = filesNew + filesUpdated + filesUpToDate + filesFiltered;
    return new IndexReport(
            rootDirectory, filesNew, filesUpdated, filesUpToDate,
            filesFiltered, total, directoriesVisited, errors, elapsed);
  }
}