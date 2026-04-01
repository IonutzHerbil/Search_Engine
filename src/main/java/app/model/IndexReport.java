package app.model;

public record IndexReport(
    String rootDir,
    int filesNew,
    int filesUpdated,
    int filesUpToDate,
    int filesFiltered,
    int filesTotal,
    int directoriesVisited,
    int errors,
    double elapsedSeconds) {

  public int filesIndexed() {
    return filesNew + filesUpdated;
  }
}
