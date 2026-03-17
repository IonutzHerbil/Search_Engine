package vision.model;

public record IndexReport(
        String rootDir,
        int filesFound,
        int directoriesVisited,
        int skipped,
        int errors,
        double elapsedSeconds
) {}
