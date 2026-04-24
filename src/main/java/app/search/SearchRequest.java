package app.search;

import java.util.List;

public record SearchRequest(String terms, List<String> extensions, List<String> directories) {}
