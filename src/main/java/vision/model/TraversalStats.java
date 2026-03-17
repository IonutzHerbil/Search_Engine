package vision.model;

public class TraversalStats {

    private int filesFound = 0;
    private int directoriesVisited = 0;
    private int skipped = 0;
    private int errors = 0;
    private final long startTime = System.currentTimeMillis();

    public void recordFile()      {
        filesFound++;
    }
    public void recordDirectory() {
        directoriesVisited++;
    }
    public void recordSkipped()   {
        skipped++;
    }
    public void recordError()     {
        errors++;
    }

    public void printReport() {
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("--------------------------------");
        System.out.printf("Done in %.2fs%n", elapsed / 1000.0);
        System.out.println("  Files found        : " + filesFound);
        System.out.println("  Directories visited: " + directoriesVisited);
        System.out.println("  Skipped            : " + skipped);
        System.out.println("  Errors             : " + errors);
        System.out.println("--------------------------------");
    }
}