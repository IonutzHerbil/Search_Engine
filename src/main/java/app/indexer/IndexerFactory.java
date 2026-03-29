package app.indexer;

import app.config.IndexConfig;
import app.db.FileRepository;
import app.processor.ContentExtractor;

public class IndexerFactory {

  private final FileRepository repository;
  private final ContentExtractor extractor;

  public IndexerFactory(FileRepository repository, ContentExtractor extractor) {
    this.repository = repository;
    this.extractor = extractor;
  }

  public FileIndexer create(String path) {
    IndexConfig config = IndexConfig.fromArgs(new String[] {path});
    FileFilter filter = new FileFilter(config);
    return new FileIndexer(config, repository, filter, extractor);
  }
}
