# Search Engine: Architecture Document

Software Design 2026 | Iteration 1

---

## 1. System Context

A local file search tool. Point it at a directory, it indexes everything, then
answers keyword queries with ranked results and file previews. No server, no
background process, no internet. The index is a single file on disk that persists
between runs, only changed files get re-processed.

```mermaid
graph TD
    User("User")

    subgraph Machine["Local Machine"]
        Engine["Search Engine\n[Java 17]"]
        FS["Local Filesystem"]
        DB[("SQLite Index\n[search_index.db]")]
    end

    User -->|"runs, configures, queries"| Engine
    Engine -->|"traverses and reads"| FS
    Engine -->|"persists index"| DB
```

**User** —> points the tool at a directory, waits for indexing to finish, types
queries either in the terminal or through the GUI.

**Local Filesystem** —> read-only from the engine's perspective. Source of all
file metadata and content.

**SQLite** —> single `.db` file on disk. FTS5 extension gives full-text search and
BM25 ranking with no extra dependencies.

---

## 2. Containers

Two frontends, one core, one database. CLI and GUI both delegate to the same core
logic: neither owns any indexing or search code directly.

```mermaid
graph TD
    User("User")

    subgraph Machine["Local Machine"]
        CLI["CLI Application\n[Java 17]"]
        GUI["GUI Application\n[Java 17, JavaFX]"]
        Core["search-engine.jar\n[Core Logic]"]
        DB[("SQLite Database\n[search_index.db]")]
        FS["Local Filesystem"]
    end

    User -->|"arguments + stdin"| CLI
    User -->|"search bar + clicks"| GUI
    CLI -->|"delegates to"| Core
    GUI -->|"delegates to"| Core
    Core -->|"JDBC via sqlite-jdbc"| DB
    Core -->|"java.nio.file"| FS
```

| Container | Technology | Responsibility |
|-----------|------------|----------------|
| **CLI Application** | Java 17 | Argument parsing, indexing run, interactive search loop, printed results |
| **GUI Application** | Java 17, JavaFX | Search bar, ranked results list, file preview panel |
| **search-engine.jar** | Java 17, Maven | All core logic — crawling, filtering, extracting, indexing, searching |
| **search_index.db** | SQLite 3, FTS5 | File metadata and full-text inverted index |

---

## 3. Components

```mermaid
graph TD
    FS["Filesystem"]
    DB[("SQLite")]

    subgraph Core["search-engine.jar"]
        Config["IndexConfig\nconfiguration"]
        Filter["FileFilter\nskip rules"]
        FileTypes["FileTypes\nextension check"]
        Extractor["ContentExtractor\ntext reader"]
        Factory["IndexerFactory\nindexer builder"]
        Indexer["FileIndexer\ntraversal + pipeline"]
        Repo["FileRepository\nSQL queries"]
        DBConn["Database\nconnection + schema"]
        Parser["SearchRequestParser\nquery parsing"]
        Search["SearchEngine\nquery + ranking"]
        CLIClass["CLI\nterminal interface"]
        GUICtrl["SearchController\nJavaFX controller"]
        SearchVM["SearchViewModel\nsearch state"]
        IndexVM["IndexViewModel\nindex state"]
        Highlighter["TextHighlighter\nterm highlighting"]
        CellFactory["ResultCellFactory\nresult rendering"]
    end

    Config --> Factory
    Config --> Indexer
    Filter --> Indexer
    FileTypes --> Extractor
    Extractor --> Factory
    Factory --> Indexer
    Indexer -->|"walkFileTree"| FS
    Indexer -->|"upsert / getLastModifiedMap"| Repo
    Repo --> DBConn
    DBConn --> DB
    Parser --> Search
    Repo --> Search
    Search --> SearchVM
    Repo --> SearchVM
    Factory --> IndexVM
    SearchVM --> GUICtrl
    IndexVM --> GUICtrl
    Highlighter --> GUICtrl
    CellFactory --> GUICtrl
    Search --> CLIClass
    Factory --> CLIClass
```

| Component | Package | Responsibility |
|-----------|---------|----------------|
| **IndexConfig** | `app.config` | Immutable record — root path, ignore patterns, db path, report format. Built from CLI args or custom parameters |
| **FileFilter** | `app.indexer` | Regex patterns compiled once at startup, tested against every directory name and file extension |
| **FileTypes** | `app.util` | Single source of truth for which extensions are treated as readable text |
| **ContentExtractor** | `app.processor` | Reads UTF-8 text files, returns full content and a 3-line preview. Binary files get null |
| **FileIndexer** | `app.indexer` | Drives the traversal. `FileFilter` and `ContentExtractor` injected. Skips unchanged files by comparing timestamps |
| **IndexerFactory** | `app.indexer` | Builds `FileIndexer` instances from a path and optional ignore rules |
| **Database** | `app.db` | Owns the SQLite connection and creates the schema on first run |
| **FileRepository** | `app.db` | All file-related SQL — `getLastModifiedMap`, `upsertNoCommit`, `search`, `deleteStale`, `getDistinctExtensions` |
| **SearchRequestParser** | `app.search` | Parses raw input into a `SearchRequest`, extracts `ext:` and `dir:` filters |
| **SearchEngine** | `app.search` | Delegates to `SearchRequestParser`, calls `FileRepository`, returns BM25-ranked results |
| **CLI** | `app.cli` | Runs the interactive search loop, formats and prints results |
| **SearchController** | `app.gui` | JavaFX controller — handles search bar input, result selection, preview display, and indexing controls |
| **SearchViewModel** | `app.gui` | Holds search results, pagination state, sort order, and available extensions |
| **IndexViewModel** | `app.gui` | Manages indexing state — progress status, report, and completion callback |
| **ResultCellFactory** | `app.gui` | Custom `ListCell` rendering for search results — name, path, extension, size, date, snippet |
| **TextHighlighter** | `app.gui` | Splits text into plain and highlighted `Text` nodes based on search terms |

---

## 4. Classes

```mermaid
classDiagram
    class IndexConfig {
        +Path rootDirectory
        +Set~String~ ignoredDirectoryPatterns
        +Set~String~ ignoredExtensions
        +String dbPath
        +String reportFormat
        +fromArgs(String[] args)$ IndexConfig
        +custom(String, Set, Set)$ IndexConfig
    }

    class FileTypes {
        +isText(String extension)$ boolean
        +parseExtension(String name)$ String
    }

    class FileRecord {
        +String path
        +String name
        +String extension
        +long sizeBytes
        +long lastModified
        +String preview
        +String content
    }

    class FileFilter {
        -List~Pattern~ dirPatterns
        -IndexConfig config
        +shouldSkipDir(String name) boolean
        +shouldSkipFile(String extension) boolean
    }

    class ContentExtractor {
        +extract(Path file, BasicFileAttributes attrs) FileRecord
    }

    class IndexerFactory {
        -FileRepository repository
        -ContentExtractor extractor
        +create(String path) FileIndexer
        +create(String path, Set ignoredDirs, Set ignoredExts) FileIndexer
    }

    class FileIndexer {
        -IndexConfig config
        -FileRepository repository
        -FileFilter filter
        -ContentExtractor extractor
        +FileIndexer(IndexConfig, FileRepository, FileFilter, ContentExtractor)
        +index(Consumer~String~ onFileIndexed) IndexReport
        +getRootDirectory() Path
    }

    class TraversalStats {
        -int filesNew
        -int filesUpdated
        -int filesUpToDate
        -int filesFiltered
        -int directoriesVisited
        -int errors
        +recordNewFile()
        +recordUpdatedFile()
        +recordUpToDate()
        +recordFiltered()
        +recordDirectory()
        +recordError()
        +toReport(String root) IndexReport
    }

    class IndexReport {
        +String rootDir
        +int filesNew
        +int filesUpdated
        +int filesUpToDate
        +int filesFiltered
        +int filesTotal
        +int directoriesVisited
        +int errors
        +double elapsedSeconds
        +filesIndexed() int
    }

    class Database {
        -Connection connection
        +Database(String dbPath)
        +getConnection() Connection
        +close()
    }

    class FileRepository {
        -Connection connection
        +FileRepository(Database db)
        +getLastModifiedMap(String rootPath) Map
        +upsertNoCommit(FileRecord record)
        +commit()
        +deleteStale(String rootPath)
        +getDistinctExtensions() List~String~
        +search(String query, String ext, String dir, int limit, int offset, SortOrder sort) List~SearchResult~
    }

    class SortOrder {
        <<enumeration>>
        RELEVANCE
        DATE
        SIZE
    }

    class SearchRequest {
        +String terms
        +String extension
        +String directory
    }

    class SearchRequestParser {
        +parse(String raw) SearchRequest
    }

    class SearchEngine {
        -FileRepository repository
        -SearchRequestParser parser
        -int DEFAULT_LIMIT
        +SearchEngine(FileRepository repository)
        +search(String raw) List~SearchResult~
        +search(String raw, int limit, int offset) List~SearchResult~
        +search(String raw, int limit, int offset, SortOrder sort) List~SearchResult~
    }

    class SearchResult {
        +String path
        +String name
        +String extension
        +String snippet
        +double score
        +long lastModified
        +long sizeBytes
    }

    class CLI {
        -IndexerFactory factory
        -SearchEngine engine
        +CLI(IndexerFactory factory, SearchEngine engine)
        +run()
    }

    class MainApp {
        +start(Stage stage)
        +main(String[] args)$
    }

    class SearchController {
        -SearchViewModel searchVM
        -IndexViewModel indexVM
        +init(IndexerFactory, SearchEngine, FileRepository)
        +onSearch()
        +onIndex()
        +onBrowse()
        +onLoadMore()
        +onExportReport()
        +onToggleSettings()
        +onClearFilters()
    }

    class SearchViewModel {
        -SearchEngine engine
        -FileRepository repository
        -ObservableList~SearchResult~ results
        -ObservableList~String~ availableExtensions
        +search(String terms, String ext, String dir)
        +loadMore()
        +refreshExtensions()
        +setSortOrder(SortOrder order)
    }

    class IndexViewModel {
        -IndexerFactory factory
        -StringProperty status
        -BooleanProperty indexing
        -ObjectProperty~IndexReport~ report
        +index(String path, Set ignoredDirs, Set ignoredExts)
        +setOnIndexComplete(Runnable callback)
    }

    class ResultCellFactory {
        +call(ListView) ListCell~SearchResult~
    }

    class TextHighlighter {
        +highlight(String content, String query)$ List~Node~
    }

    IndexerFactory --> FileIndexer
    FileIndexer --> FileFilter
    FileIndexer --> ContentExtractor
    FileIndexer --> FileRepository
    FileIndexer ..> FileRecord
    FileIndexer ..> TraversalStats
    TraversalStats ..> IndexReport
    ContentExtractor ..> FileRecord
    ContentExtractor --> FileTypes
    FileRepository --> Database
    FileRepository ..> SearchResult
    SearchEngine --> FileRepository
    SearchEngine --> SearchRequestParser
    SearchRequestParser ..> SearchRequest
    SearchEngine ..> SearchResult
    CLI --> IndexerFactory
    CLI --> SearchEngine
    MainApp --> SearchController
    SearchController --> SearchViewModel
    SearchController --> IndexViewModel
    SearchController --> TextHighlighter
    SearchController --> ResultCellFactory
    SearchViewModel --> SearchEngine
    SearchViewModel --> FileRepository
    IndexViewModel --> IndexerFactory
```

GUI layout: split pane: left panel has index controls (directory path, browse, index
button, configurable ignore rules, report format export), a search bar with live search,
extension and directory filters, sort order choice, and a paginated results list. Right
panel shows the selected file with two tabs: Preview (first 3 lines, highlighted) and
Full File (complete content, highlighted). A report strip shows indexing statistics after
each run.

---

## Runtime Behaviour

### Indexing

`IndexerFactory` builds a `FileIndexer` from a path and ignore rules. `FileIndexer`
preloads all known timestamps via `FileRepository.getLastModifiedMap`, then walks the
tree. Each file is checked against `FileFilter` first, then against the preloaded
timestamps. Only new or modified files get extracted and written to the index. Writes
are batched — `upsertNoCommit` per file, `commit` every 200 files. After traversal,
`deleteStale` removes entries for files that no longer exist on disk.

```mermaid
sequenceDiagram
    actor User
    participant IndexerFactory
    participant FileIndexer
    participant FileFilter
    participant FileRepository
    participant ContentExtractor

    User->>IndexerFactory: create(path, ignoredDirs, ignoredExts)
    IndexerFactory-->>User: FileIndexer
    User->>FileIndexer: index(onFileIndexed)
    FileIndexer->>FileRepository: getLastModifiedMap(root)
    FileRepository-->>FileIndexer: Map of path → timestamp
    loop each file in tree
        FileIndexer->>FileFilter: shouldSkip(file)?
        FileFilter-->>FileIndexer: skip / continue
        Note over FileIndexer: compare timestamp — skip if unchanged
        FileIndexer->>ContentExtractor: extract(file)
        ContentExtractor-->>FileIndexer: FileRecord
        FileIndexer->>FileRepository: upsertNoCommit(record)
        Note over FileIndexer: commit every 200 files
    end
    FileIndexer->>FileRepository: commit (remaining)
    FileIndexer->>FileRepository: deleteStale(root)
    FileIndexer-->>User: IndexReport
```

### Search

Raw input goes into `SearchEngine`, `SearchRequestParser` breaks it into terms and
optional `ext:` and `dir:` filters, `FileRepository` runs the FTS5 query, results come
back ranked by BM25 (or sorted by date/size).

```mermaid
sequenceDiagram
    actor User
    participant SearchEngine
    participant SearchRequestParser
    participant FileRepository

    User->>SearchEngine: search(raw, limit, offset, sort)
    SearchEngine->>SearchRequestParser: parse(raw)
    SearchRequestParser-->>SearchEngine: SearchRequest(terms, ext, dir)
    SearchEngine->>FileRepository: search(terms, ext, dir, limit, offset, sort)
    FileRepository-->>SearchEngine: ranked results
    SearchEngine-->>User: List~SearchResult~
```

---

## Deployment

Runs on any machine with JRE 17. sqlite-jdbc is bundled in the JAR so nothing
needs installing. Database file is auto-created on first run.

```mermaid
graph TD
    subgraph dev["Developer Machine"]
        JAR["search-engine.jar"]
        DB["search_index.db"]
        FS["Target Directory"]
    end

    JAR -->|"reads and writes"| DB
    JAR -->|"traverses"| FS
```