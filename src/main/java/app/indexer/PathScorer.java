package app.indexer;

import java.util.Map;

public final class PathScorer {

  private static final Map<String, Double> EXT_WEIGHT =
      Map.ofEntries(
          Map.entry("java", 1.20),
          Map.entry("kt", 1.20),
          Map.entry("py", 1.20),
          Map.entry("ts", 1.15),
          Map.entry("js", 1.10),
          Map.entry("cpp", 1.15),
          Map.entry("c", 1.15),
          Map.entry("h", 1.05),
          Map.entry("cs", 1.15),
          Map.entry("md", 1.10),
          Map.entry("sql", 1.05),
          Map.entry("html", 0.90),
          Map.entry("css", 0.85),
          Map.entry("json", 0.90),
          Map.entry("xml", 0.85),
          Map.entry("yml", 0.90),
          Map.entry("yaml", 0.90),
          Map.entry("properties", 0.80),
          Map.entry("txt", 0.90),
          Map.entry("sh", 1.00),
          Map.entry("bat", 0.85));

  private PathScorer() {}

  public static double score(String absolutePath, String extension) {
    double score = 1.0;
    String norm = absolutePath.replace('\\', '/').toLowerCase();

    score *= EXT_WEIGHT.getOrDefault(extension.toLowerCase(), 0.70);

    long depth = norm.chars().filter(c -> c == '/').count();
    score -= Math.max(0L, depth - 5) * 0.03;

    if (norm.contains("/src/main/")) score += 0.40;
    else if (norm.contains("/src/")) score += 0.20;
    if (norm.contains("/lib/")) score += 0.10;
    if (norm.contains("/core/")) score += 0.10;
    if (norm.contains("/test/") || norm.contains("/tests/")) score -= 0.10;
    if (norm.contains("/vendor/")) score -= 0.25;
    if (norm.contains("/generated/") || norm.contains("/gen/")) score -= 0.20;
    if (norm.contains("/node_modules/")) score -= 0.50;

    return Math.max(0.0, Math.min(score * 5.0, 10.0));
  }
}
