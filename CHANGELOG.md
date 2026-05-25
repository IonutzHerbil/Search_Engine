# Changelog

---

## [3.0.0] - 2026-05-19 - Iteration 3

### Added
- Pre-commit and pre-push hooks in .githooks/ ([c0ccf56](https://github.com/IonutzHerbil/Search_Engine/commit/c0ccf56))
- CHANGELOG.md and semantic versioning ([cf1a2c9](https://github.com/IonutzHerbil/Search_Engine/commit/cf1a2c9))
- dominantColor field to file model and database schema ([e3dc448](https://github.com/IonutzHerbil/Search_Engine/commit/e3dc448))
- Strategy Pattern for file extraction with TextExtractionStrategy and ImageExtractionStrategy ([c2b48c0](https://github.com/IonutzHerbil/Search_Engine/commit/c2b48c0))
- Image dominant color extraction via pixel sampling ([c2b48c0](https://github.com/IonutzHerbil/Search_Engine/commit/c2b48c0))
- color: query qualifier for filtering by dominant image color ([549b5de](https://github.com/IonutzHerbil/Search_Engine/commit/549b5de))
- Dominant color chip in result cells for image files ([14a3dc2](https://github.com/IonutzHerbil/Search_Engine/commit/14a3dc2))
- Image preview in Full File tab ([a3c4b5b](https://github.com/IonutzHerbil/Search_Engine/commit/a3c4b5b))
- Color filter dropdown with visual swatches ([4c5f15b](https://github.com/IonutzHerbil/Search_Engine/commit/4c5f15b))
- Decorator Pipeline: SanitizationDecorator, SynonymDecorator, LogicDecorator ([4d9e009](https://github.com/IonutzHerbil/Search_Engine/commit/4d9e009))
- Synonym expansion via synonyms.properties ([4d9e009](https://github.com/IonutzHerbil/Search_Engine/commit/4d9e009))
- Context-aware widget strip: GalleryWidget and LogAnalyzerWidget ([40acfb0](https://github.com/IonutzHerbil/Search_Engine/commit/40acfb0))
- Producer-Consumer indexing with parallel readers and single writer thread ([69523f4](https://github.com/IonutzHerbil/Search_Engine/commit/69523f4))

### Fixed
- Hyphenated terms consumed as single tokens in decorator pipeline ([0d7f402](https://github.com/IonutzHerbil/Search_Engine/commit/0d7f402))
- PRAGMA synchronous = NORMAL restored

### Changed
- Decorator pipeline wired into SearchEngine before parser ([41ff564](https://github.com/IonutzHerbil/Search_Engine/commit/41ff564))
- Color tokens extracted into theme.css ([8a9d002](https://github.com/IonutzHerbil/Search_Engine/commit/8a9d002))
- Indexing batch size increased from 200 to 500

### Tests
- QueryPipelineTest: unit tests for decorator pipeline
- Integration tests updated for dominantColor field ([c45fd78](https://github.com/IonutzHerbil/Search_Engine/commit/c45fd78))
---

## [2.0.0] - 2026-04-29 - Iteration 2

### Added
- Apache Tika integration for document parsing (PDF, DOCX, HTML) and binary file safety ([39212ca](https://github.com/IonutzHerbil/Search_Engine/commit/39212ca))
- Live FileWatcher using Java WatchService, auto-indexes file changes after initial index ([e3eb10d](https://github.com/IonutzHerbil/Search_Engine/commit/e3eb10d))
- Search Analytics Dashboard: session stats and DB stats ([9db0210](https://github.com/IonutzHerbil/Search_Engine/commit/9db0210))
- Observer Pattern: SearchObserver interface, SearchEngine as subject, SearchHistoryService as observer ([4a04f0a](https://github.com/IonutzHerbil/Search_Engine/commit/4a04f0a))
- Strategy Pattern for ranking: RankingStrategy interface replaces SortOrder enum ([7229631](https://github.com/IonutzHerbil/Search_Engine/commit/7229631))
- PathScorer: 0-10 relevance score per file computed at index time ([e058633](https://github.com/IonutzHerbil/Search_Engine/commit/e058633))
- pathScore stored in DB with B-tree index ([78a1235](https://github.com/IonutzHerbil/Search_Engine/commit/78a1235))
- pathScore displayed in result cells ([1d22342](https://github.com/IonutzHerbil/Search_Engine/commit/1d22342))
- Path Score sort option added to dropdown ([b06e309](https://github.com/IonutzHerbil/Search_Engine/commit/b06e309))
- Boolean operators, wildcard, and phrase search via FTS5 native syntax ([1ac160c](https://github.com/IonutzHerbil/Search_Engine/commit/1ac160c))
- Term highlighting for boolean operators and phrases ([6104ee2](https://github.com/IonutzHerbil/Search_Engine/commit/6104ee2))
- Query qualifiers: ext:, path:, name:, content: ([e045f14](https://github.com/IonutzHerbil/Search_Engine/commit/e045f14))
- name: qualifier ([03aa372](https://github.com/IonutzHerbil/Search_Engine/commit/03aa372))
- OR semantics for ext:, AND semantics for path: ([ae1c28a](https://github.com/IonutzHerbil/Search_Engine/commit/ae1c28a))
- Search history autocomplete suggestions ([e3f6042](https://github.com/IonutzHerbil/Search_Engine/commit/e3f6042))
- History-based result boosting with substring match across past queries ([e3f6042](https://github.com/IonutzHerbil/Search_Engine/commit/e3f6042))
- Persistent pathScore boost in DB for every returned result ([48a2740](https://github.com/IonutzHerbil/Search_Engine/commit/48a2740))
- BM25 column weights: filename matches worth 10x content matches ([cc81ff8](https://github.com/IonutzHerbil/Search_Engine/commit/cc81ff8))
- CamelCase normalization for filename indexing ([211d2b2](https://github.com/IonutzHerbil/Search_Engine/commit/211d2b2))
- Analytics DB queries: countFiles(), topExtensions(n), averagePathScore() ([9db0210](https://github.com/IonutzHerbil/Search_Engine/commit/9db0210))
- Alphabetical sort strategy ([7229631](https://github.com/IonutzHerbil/Search_Engine/commit/7229631))

### Fixed
- Filter-only queries (e.g. ext:java alone) now route to metadataSearch() fallback ([b19ec8c](https://github.com/IonutzHerbil/Search_Engine/commit/b19ec8c))
- ORDER BY 0 crash in metadataSearch corrected to ORDER BY r ([cc81ff8](https://github.com/IonutzHerbil/Search_Engine/commit/cc81ff8))
- Qualified tokens (ext:java, path:src) now consumed as single tokens ([cecdd62](https://github.com/IonutzHerbil/Search_Engine/commit/cecdd62))
- Binary and oversized files no longer block the JavaFX UI ([39212ca](https://github.com/IonutzHerbil/Search_Engine/commit/39212ca))
- Stale file preview race condition fixed with token identity check ([4e572c8](https://github.com/IonutzHerbil/Search_Engine/commit/4e572c8))
- Filter-only queries blocked by UI early return ([cc81ff8](https://github.com/IonutzHerbil/Search_Engine/commit/cc81ff8))
- Filter-only queries blocked by engine early return ([211d2b2](https://github.com/IonutzHerbil/Search_Engine/commit/211d2b2))
- dir: qualifier renamed to path: ([ece1a99](https://github.com/IonutzHerbil/Search_Engine/commit/ece1a99))
- testHistoryBehaviorBoosting pre-polluting search history ([a791fbc](https://github.com/IonutzHerbil/Search_Engine/commit/a791fbc))

### Changed
- UI styling: opposite corners rounded & square for visual consistency ([dd2f5f3](https://github.com/IonutzHerbil/Search_Engine/commit/dd2f5f3))
- Search and file loading moved to virtual threads ([4e572c8](https://github.com/IonutzHerbil/Search_Engine/commit/4e572c8))
- FTS5 query restructured as subquery with inner LIMIT 5000 ([4e572c8](https://github.com/IonutzHerbil/Search_Engine/commit/4e572c8))
- SortOrder enum deleted, replaced by RankingStrategy interface constants ([7229631](https://github.com/IonutzHerbil/Search_Engine/commit/7229631))
- SearchRequest changed from single strings to List<String> for extensions and directories ([ae1c28a](https://github.com/IonutzHerbil/Search_Engine/commit/ae1c28a))
- Directory filter changed from prefix match to substring match ([b19ec8c](https://github.com/IonutzHerbil/Search_Engine/commit/b19ec8c))
- Full file preview now reads from DB via getFullContent() instead of disk ([39212ca](https://github.com/IonutzHerbil/Search_Engine/commit/39212ca))
- Java compiler target upgraded from 17 to 21 ([16764fd](https://github.com/IonutzHerbil/Search_Engine/commit/16764fd))

### Database
- Added pathScore REAL NOT NULL DEFAULT 0.0 column to files table ([78a1235](https://github.com/IonutzHerbil/Search_Engine/commit/78a1235))
- Safe ALTER TABLE migration for existing databases ([78a1235](https://github.com/IonutzHerbil/Search_Engine/commit/78a1235))
- B-tree indexes on extension, lastModified DESC, sizeBytes DESC, pathScore DESC ([4e572c8](https://github.com/IonutzHerbil/Search_Engine/commit/4e572c8))
- WAL journal mode, 32MB page cache ([4e572c8](https://github.com/IonutzHerbil/Search_Engine/commit/4e572c8))

### Tests
- Unit tests: SearchRequestParserTest, PathScorerTest, SearchHistoryServiceTest, RankingStrategyTest ([f4cef0d](https://github.com/IonutzHerbil/Search_Engine/commit/f4cef0d))
- Integration tests: RankingIntegrationTest with in-memory SQLite ([1dc3994](https://github.com/IonutzHerbil/Search_Engine/commit/1dc3994))
- ContentExtractorTest for Tika parsing and binary safety ([39212ca](https://github.com/IonutzHerbil/Search_Engine/commit/39212ca))

---

## [1.0.0] - 2026-04-01 - Iteration 1

### Added
- JavaFX GUI with NordDark theme, SplitPane layout, custom result cells, file preview panel
- MVVM architecture: SearchViewModel with ObservableList, IndexViewModel with status binding ([dd927d0](https://github.com/IonutzHerbil/Search_Engine/commit/dd927d0))
- SQLite FTS5 full-text search: WAL mode, 32MB cache, B-tree indexes ([4e572c8](https://github.com/IonutzHerbil/Search_Engine/commit/4e572c8))
- Incremental indexing: skip unchanged files by lastModified timestamp comparison ([6bc7de5](https://github.com/IonutzHerbil/Search_Engine/commit/6bc7de5))
- Batch DB commits every 200 files ([8c4e7d9](https://github.com/IonutzHerbil/Search_Engine/commit/8c4e7d9))
- Background indexing on JavaFX Task thread with progress bar ([5377d86](https://github.com/IonutzHerbil/Search_Engine/commit/5377d86))
- Export report in TEXT and JSON formats ([d20aeed](https://github.com/IonutzHerbil/Search_Engine/commit/d20aeed))
- Live search with 350ms debounce ([76399e0](https://github.com/IonutzHerbil/Search_Engine/commit/76399e0))
- Extension filter dropdown, directory text filter ([6bc2013](https://github.com/IonutzHerbil/Search_Engine/commit/6bc2013))
- Load More pagination ([c19fb89](https://github.com/IonutzHerbil/Search_Engine/commit/c19fb89))
- File preview: stored 3-line snippet shown instantly, full file loaded asynchronously
- TextHighlighter: matched terms highlighted in preview using JavaFX TextFlow
- Stale file cleanup after re-index ([5b3df73](https://github.com/IonutzHerbil/Search_Engine/commit/5b3df73))
- Configurable ignore rules editable in UI ([36d8758](https://github.com/IonutzHerbil/Search_Engine/commit/36d8758))
- IndexerFactory (Factory Pattern) ([c53a92b](https://github.com/IonutzHerbil/Search_Engine/commit/c53a92b))
- FileTypes utility class ([f5c8d8e](https://github.com/IonutzHerbil/Search_Engine/commit/f5c8d8e))
- BM25 relevance score display in result cells ([1ab1af2](https://github.com/IonutzHerbil/Search_Engine/commit/1ab1af2))
- File sorting by relevance, date, and size ([21c3092](https://github.com/IonutzHerbil/Search_Engine/commit/21c3092))
- dir: filter for directory-based search ([96d89d6](https://github.com/IonutzHerbil/Search_Engine/commit/96d89d6))

### Performance
- Virtual threads for async search ([4e572c8](https://github.com/IonutzHerbil/Search_Engine/commit/4e572c8))
- PRAGMA synchronous = NORMAL
- PRAGMA cache_size = -32000
- PRAGMA journal_mode = WAL ([f03c89b](https://github.com/IonutzHerbil/Search_Engine/commit/f03c89b))

### Infrastructure
- Google Java Style formatter with Spotless ([104f4b1](https://github.com/IonutzHerbil/Search_Engine/commit/104f4b1))
- JavaFX dependencies and Maven plugins ([11d74ef](https://github.com/IonutzHerbil/Search_Engine/commit/11d74ef))

---

## [0.1.0] - 2026-03-17 - Initial Setup

### Added
- Maven project, SQLite JDBC dependency
- FileIndexer with recursive directory traversal ([649bb2b](https://github.com/IonutzHerbil/Search_Engine/commit/649bb2b))
- FileFilter with regex-based ignore rules ([eb52a7f](https://github.com/IonutzHerbil/Search_Engine/commit/eb52a7f))
- ContentExtractor with text extraction and 3-line preview ([eb52a7f](https://github.com/IonutzHerbil/Search_Engine/commit/eb52a7f))
- IndexConfig with runtime defaults ([eb52a7f](https://github.com/IonutzHerbil/Search_Engine/commit/eb52a7f))
- TraversalStats and IndexReport ([eb52a7f](https://github.com/IonutzHerbil/Search_Engine/commit/eb52a7f))
- FileRecord and SearchResult model records ([6fa946e](https://github.com/IonutzHerbil/Search_Engine/commit/6fa946e))
- CLI for indexing and searching ([a1f2778](https://github.com/IonutzHerbil/Search_Engine/commit/a1f2778))
- Symlink loop detection ([eb52a7f](https://github.com/IonutzHerbil/Search_Engine/commit/eb52a7f))
- SearchEngine with SearchRequestParser ([a1f2778](https://github.com/IonutzHerbil/Search_Engine/commit/a1f2778))
- SQLite database layer with FTS5 schema ([a72e777](https://github.com/IonutzHerbil/Search_Engine/commit/a72e777))
- Database and FileRepository split from DatabaseManager ([d1e838f](https://github.com/IonutzHerbil/Search_Engine/commit/d1e838f))