package app.indexer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PathScorerTest {

  @Test
  void scoreIsWithinBounds() {
    double s = PathScorer.score("C:/projects/myapp/src/main/java/Foo.java", "java");
    assertTrue(s >= 0.0 && s <= 10.0, "score out of [0,10]: " + s);
  }

  @Test
  void srcMainBoostHigherThanRoot() {
    double withSrcMain = PathScorer.score("/home/user/proj/src/main/java/App.java", "java");
    double withoutSrc = PathScorer.score("/home/user/proj/App.java", "java");
    assertTrue(withSrcMain > withoutSrc);
  }

  @Test
  void testDirPenalty() {
    double main = PathScorer.score("/proj/src/main/java/Foo.java", "java");
    double test = PathScorer.score("/proj/src/test/java/FooTest.java", "java");
    assertTrue(main > test);
  }

  @Test
  void nodeModulesPenalty() {
    double normal = PathScorer.score("/proj/src/index.js", "js");
    double nodeModule = PathScorer.score("/proj/node_modules/index.js", "js");
    assertTrue(normal > nodeModule);
  }

  @Test
  void deepPathPenalty() {
    double shallow = PathScorer.score("/a/b/c/Foo.java", "java");
    double deep = PathScorer.score("/a/b/c/d/e/f/g/h/i/j/k/Foo.java", "java");
    assertTrue(shallow > deep);
  }

  @Test
  void unknownExtensionLowerScore() {
    double java = PathScorer.score("/proj/src/Foo.java", "java");
    double unknown = PathScorer.score("/proj/src/Foo.xyz", "xyz");
    assertTrue(java > unknown);
  }

  @Test
  void zeroFloor() {
    String path = "/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/node_modules/vendor/generated/Foo.xyz";
    double s = PathScorer.score(path, "xyz");
    assertTrue(s >= 0.0);
  }

  @Test
  void libDirBoost() {
    double withLib = PathScorer.score("/proj/lib/utils.java", "java");
    double withoutLib = PathScorer.score("/proj/stuff/utils.java", "java");
    assertTrue(withLib > withoutLib);
  }
}
