package vision;

import vision.indexer.FileIndexer;

import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        FileIndexer indexer = new FileIndexer(Paths.get(System.getProperty("user.home")));
        indexer.index();
    }
}