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

Seven components inside the JAR. The indexing pipeline flows through `FileIndexer`.
`SearchEngine` is completely independent, used by both CLI and GUI.
`SearchController` wires the GUI to the core.

```mermaid
graph TD
    FS["Filesystem"]
    DB[("SQLite")]

    subgraph Core["search-engine.jar"]
        Config["IndexConfig\nconfiguration"]
        Filter["FileFilter\nskip rules"]
        FileTypes["FileTypes\nextension check"]
        Extractor["ContentExtractor\ntext reader"]
        Indexer["FileIndexer\ntraversal + pipeline"]
        Repo["FileRepository\nSQL queries"]
        DBConn["Database\nconnection + schema"]
        Parser["SearchRequestParser\nquery parsing"]
        Search["SearchEngine\nquery + ranking"]
        CLIClass["CLI\nterminal interface"]
        GUICtrl["SearchController\nJavaFX controller"]
    end

    Config --> Indexer
    Filter --> Indexer
    FileTypes --> Extractor
    Extractor --> Indexer
    Indexer -->|"walkFileTree"| FS
    Indexer -->|"upsert / getLastModified"| Repo
    Repo --> DBConn
    DBConn --> DB
    Parser --> Search
    Repo --> Search
    Search --> CLIClass
    Indexer --> CLIClass
    Search --> GUICtrl
    Indexer --> GUICtrl
```

| Component | Package | Responsibility |
|-----------|---------|----------------|
| **IndexConfig** | `app.config` | Immutable record — root path, ignore patterns, db path, report format. Built from CLI args |
| **FileFilter** | `app.indexer` | Regex patterns compiled once at startup, tested against every directory name and file extension |
| **FileTypes** | `app.util` | Single source of truth for which extensions are treated as readable text |
| **ContentExtractor** | `app.processor` | Reads UTF-8 text files, returns full content and a 3-line preview. Binary files get null |
| **FileIndexer** | `app.indexer` | Drives the traversal. `FileFilter` and `ContentExtractor` injected. Skips unchanged files by comparing timestamps |
| **Database** | `app.db` | Owns the SQLite connection and creates the schema on first run |
| **FileRepository** | `app.db` | All file-related SQL — `getLastModified`, `upsert`, `search` |
| **SearchRequestParser** | `app.search` | Parses raw input into a `SearchRequest`, extracts `ext:` filters |
| **SearchEngine** | `app.search` | Delegates to `SearchRequestParser`, calls `FileRepository`, returns BM25-ranked results |
| **CLI** | `app.cli` | Runs the interactive search loop, formats and prints results |
| **SearchController** | `app.gui` | JavaFX controller — handles search bar input, result selection, and preview display |

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
    }

    class FileTypes {
        +isText(String extension)$ boolean
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

    class FileIndexer {
        -IndexConfig config
        -FileRepository repository
        -FileFilter filter
        -ContentExtractor extractor
        +FileIndexer(IndexConfig, FileRepository, FileFilter, ContentExtractor)
        +index()
    }

    class TraversalStats {
        -int filesFound
        -int directoriesVisited
        -int skipped
        -int errors
        +recordFile()
        +recordDirectory()
        +recordSkipped()
        +recordError()
        +toReport(String root) IndexReport
    }

    class IndexReport {
        +String rootDir
        +int filesFound
        +int skipped
        +int directoriesVisited
        +int errors
        +double elapsedSeconds
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
        +getLastModified(String path) long
        +upsert(FileRecord record)
        +search(String query, String extension, int limit) List~SearchResult~
    }

    class SearchScope {
        <<enumeration>>
        ALL
        BY_EXTENSION
    }

    class SearchRequest {
        +String terms
        +SearchScope scope
        +String extension
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
        +search(String raw, int limit) List~SearchResult~
    }

    class SearchResult {
        +String path
        +String name
        +String extension
        +String snippet
        +double score
        +long lastModified
    }

    class CLI {
        -FileIndexer indexer
        -SearchEngine engine
        +CLI(FileIndexer indexer, SearchEngine engine)
        +run()
    }

    class MainApp {
        +start(Stage stage)
        +main(String[] args)$
    }

    class SearchController {
        -SearchEngine engine
        -FileIndexer indexer
        -TextField searchField
        -ListView~SearchResult~ resultsList
        -TextArea previewArea
        -Label statusLabel
        +initialize()
        +onSearch()
        +onIndex()
        +onResultSelected(SearchResult result)
    }

    class ResultCell {
        +updateItem(SearchResult result, boolean empty)
    }

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
    SearchRequest --> SearchScope
    SearchEngine ..> SearchResult
    CLI --> FileIndexer
    CLI --> SearchEngine
    MainApp --> SearchController
    SearchController --> SearchEngine
    SearchController --> FileIndexer
    SearchController --> ResultCell
```

GUI layout: search bar at the top, scrollable results list in the middle showing
filename, path and snippet per result, preview panel on the right showing the full
file content when a result is clicked, status bar at the bottom for indexing progress
and result count.

---

## Runtime Behaviour

### Indexing

`FileIndexer` walks the tree. Each file is checked against `FileFilter` first, then
against the stored timestamp via `FileRepository`. Only new or modified files get
extracted and written to the index.

```mermaid
sequenceDiagram
    actor User
    participant FileIndexer
    participant FileFilter
    participant ContentExtractor
    participant FileRepository

    User->>FileIndexer: index()
    FileIndexer->>FileFilter: shouldSkip(file)?
    FileFilter-->>FileIndexer: skip / continue
    FileIndexer->>FileRepository: getLastModified(path)
    FileRepository-->>FileIndexer: timestamp or -1
    Note over FileIndexer: if unchanged — skip
    FileIndexer->>ContentExtractor: extract(file)
    ContentExtractor-->>FileIndexer: FileRecord
    FileIndexer->>FileRepository: upsert(record)
    FileIndexer->>User: prints IndexReport
```

### Search

Raw input goes into `SearchEngine`, `SearchRequestParser` breaks it into terms and
an optional extension filter, `FileRepository` runs the FTS5 query, results come
back ranked by BM25.

```mermaid
sequenceDiagram
    actor User
    participant SearchEngine
    participant SearchRequestParser
    participant FileRepository

    User->>SearchEngine: search(raw)
    SearchEngine->>SearchRequestParser: parse(raw)
    SearchRequestParser-->>SearchEngine: SearchRequest
    SearchEngine->>FileRepository: search(terms, extension, limit)
    FileRepository-->>SearchEngine: ranked results
    SearchEngine->>User: name, path, snippet per result
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