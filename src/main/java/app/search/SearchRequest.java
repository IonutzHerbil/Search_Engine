package app.search;

public record SearchRequest(String terms, SearchScope scope, String extension, String directory) {}
