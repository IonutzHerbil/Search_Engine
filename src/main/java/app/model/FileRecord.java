package app.model;

public record FileRecord(
    String path,
    String name,
    String extension,
    long sizeBytes,
    long lastModified,
    String preview,
    String content) {}
