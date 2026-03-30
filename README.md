# Search Engine — Iteration 1

A local file search engine built for the Software Design 2026 course at TU Cluj-Napoca. Indexes files on your device and provides fast, full-text search through both a GUI and a CLI.

---

## Features

- **Recursive file traversal** with symlink loop detection
- **Full-text search** via SQLite FTS5 with BM25 ranking
- **Incremental indexing** — only re-indexes files that have changed
- **File previews** — first 3 non-blank lines of text files shown in results
- **Extension and directory filters** — narrow results by file type or path
- **Live search** — results appear as you type (GUI)
- **Dual interface** — JavaFX GUI and interactive CLI
- **Runtime configuration** — root directory, database path, report format
- **JSON and TEXT report formats** (CLI)
- **Stale entry cleanup** — removes index entries for deleted files

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Build | Maven |
| Database | SQLite + FTS5 (xerial/sqlite-jdbc 3.45.1.0) |
| GUI | JavaFX 17.0.2 + AtlantaFX NordDark |
| Formatting | Spotless + Google Java Format |

---

## Project Structure

```
src/main/java/app/
├── Main.java                  — CLI composition root
├── cli/
│   └── CLI.java               — interactive CLI
├── config/
│   └── IndexConfig.java       — runtime configuration
├── db/
│   ├── Database.java          — connection, WAL mode, schema
│   └── FileRepository.java    — all SQL queries
├── gui/
│   ├── MainApp.java           — JavaFX entry point
│   └── SearchController.java  — FXML controller
├── indexer/
│   ├── FileFilter.java        — directory and extension filtering
│   ├── FileIndexer.java       — traversal and indexing
│   └── IndexerFactory.java    — creates FileIndexer instances
├── model/
│   ├── FileRecord.java
│   ├── IndexReport.java
│   ├── SearchResult.java
│   └── TraversalStats.java
├── processor/
│   └── ContentExtractor.java  — extracts text content and previews
├── search/
│   ├── SearchEngine.java
│   ├── SearchRequest.java
│   ├── SearchRequestParser.java
│   └── SearchScope.java
└── util/
    └── FileTypes.java         — text extension registry
```

---

## Running the Project

### GUI

```bash
mvn javafx:run
```

1. Enter or browse to a directory
2. Click **Index** — live progress shown in status bar
3. Type in the search bar — results appear as you type
4. Use the extension dropdown or directory field to filter results
5. Click a result to preview it

### CLI

In IntelliJ, create a run configuration:
- Main class: `app.Main`
- Program arguments: `<directory> [database] [TEXT|JSON]`

Or via Maven:

```bash
mvn compile exec:java -Dexec.mainClass="app.Main" -Dexec.args="D:\YourDirectory"
```

On startup the CLI will prompt:
```
Directory to index (default: C:\Users\...):
Report format [TEXT/JSON] (default TEXT):
```

---

## Search Syntax

| Syntax | Example | Description |
|---|---|---|
| keyword | `main` | Full-text search |
| multi-word | `public static void` | Multi-word FTS5 search |
| extension filter | `main ext:java` | Filter by file extension |
| directory filter | `config dir:D:\src` | Filter by directory path |
| combined | `main ext:java dir:D:\src` | All filters together |

---

## Database Schema

```sql
CREATE TABLE files (
    path         TEXT PRIMARY KEY,
    name         TEXT NOT NULL,
    extension    TEXT,
    sizeBytes    INTEGER NOT NULL,
    lastModified INTEGER NOT NULL,
    preview      TEXT
);

CREATE VIRTUAL TABLE files_fts USING fts5(
    path UNINDEXED,
    name,
    content
);
```

WAL journal mode is enabled for concurrent CLI and GUI access.

---

## Ignored by Default

**Directories:** `.git`, `node_modules`, `target`, `build`, `cache`, `dist`, `AppData`, `flutter`, `scoop`

**Extensions:** `class`, `obj`, `o`, `zip`, `tar`, `gz`, `rar`, `7z`, `tmp`, `bak`, `lnk`, `db`, `sqlite`

All other file types are indexed by metadata. Text files additionally have their content indexed for full-text search.

---

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full C4 model covering system context, containers, components, and runtime sequence diagrams.
