package app.search;

import java.util.List;

public interface RankingStrategy {

  String label();

  String orderByClause();

  boolean requiresBm25();

  RankingStrategy RELEVANCE =
      new RankingStrategy() {
        public String label() {
          return "Relevance";
        }

        public String orderByClause() {
          return "ORDER BY fts.r ";
        }

        public boolean requiresBm25() {
          return true;
        }

        public String toString() {
          return label();
        }
      };

  RankingStrategy PATH_SCORE =
      new RankingStrategy() {
        public String label() {
          return "Path Score";
        }

        public String orderByClause() {
          return "ORDER BY f.pathScore DESC ";
        }

        public boolean requiresBm25() {
          return false;
        }

        public String toString() {
          return label();
        }
      };

  RankingStrategy DATE =
      new RankingStrategy() {
        public String label() {
          return "Date";
        }

        public String orderByClause() {
          return "ORDER BY f.lastModified DESC ";
        }

        public boolean requiresBm25() {
          return false;
        }

        public String toString() {
          return label();
        }
      };

  RankingStrategy SIZE =
      new RankingStrategy() {
        public String label() {
          return "Size";
        }

        public String orderByClause() {
          return "ORDER BY f.sizeBytes DESC ";
        }

        public boolean requiresBm25() {
          return false;
        }

        public String toString() {
          return label();
        }
      };

  RankingStrategy ALPHA =
      new RankingStrategy() {
        public String label() {
          return "Alphabetical";
        }

        public String orderByClause() {
          return "ORDER BY f.name COLLATE NOCASE ASC ";
        }

        public boolean requiresBm25() {
          return false;
        }

        public String toString() {
          return label();
        }
      };

  List<RankingStrategy> ALL = List.of(RELEVANCE, PATH_SCORE, DATE, SIZE, ALPHA);
}
