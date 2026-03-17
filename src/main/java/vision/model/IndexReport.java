package vision.model;

public record IndexReport(
        String rootDir,
        int filesFound,
        int skipped,
        int directoriesVisited,
        int errors,
        double elapsedSeconds
) {}
