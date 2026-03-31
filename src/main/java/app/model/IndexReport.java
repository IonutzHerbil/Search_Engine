package app.model;

public record IndexReport(
    String rootDir,
    int filesIndexed,
    int filesUpToDate,
    int filesFiltered,
    int directoriesVisited,
    int errors,
    double elapsedSeconds) {}
