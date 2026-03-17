package vision;

import vision.config.IndexConfig;
import vision.indexer.FileIndexer;

public class Main {
    public static void main(String[] args) {
        IndexConfig config = IndexConfig.fromArgs(args);

        FileIndexer indexer = new FileIndexer(config);
        indexer.index();
    }
}