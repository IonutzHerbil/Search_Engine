package app.model;

public record SearchResult(
    String path, String name, String extension, String snippet, double score, long lastModified) {}
