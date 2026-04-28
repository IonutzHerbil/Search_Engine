package app.search;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchRequestParserTest {

  private SearchRequestParser parser;

  @BeforeEach
  void setUp() {
    parser = new SearchRequestParser();
  }

  @Test
  void plainTermsPassedThrough() {
    SearchRequest r = parser.parse("hello world");
    assertEquals("hello world", r.terms());
    assertTrue(r.extensions().isEmpty());
    assertTrue(r.directories().isEmpty());
  }

  @Test
  void singleExtQualifier() {
    SearchRequest r = parser.parse("foo ext:java");
    assertEquals(1, r.extensions().size());
    assertEquals("java", r.extensions().get(0));
    assertTrue(r.terms().contains("foo"));
  }

  @Test
  void multipleExtQualifiers() {
    SearchRequest r = parser.parse("ext:java ext:kt ext:py");
    assertEquals(3, r.extensions().size());
    assertTrue(r.extensions().contains("java"));
    assertTrue(r.extensions().contains("kt"));
    assertTrue(r.extensions().contains("py"));
  }

  @Test
  void pathQualifier() {
    SearchRequest r = parser.parse("main path:src/main");
    assertEquals(1, r.directories().size());
    assertTrue(r.directories().get(0).contains("src"));
  }

  @Test
  void multiplePathQualifiers() {
    SearchRequest r = parser.parse("path:src path:test");
    assertEquals(2, r.directories().size());
  }

  @Test
  void nameQualifierRoutedToFts() {
    SearchRequest r = parser.parse("name:Main");
    assertTrue(r.terms().contains("name:Main"));
  }

  @Test
  void contentQualifierRoutedToFts() {
    SearchRequest r = parser.parse("content:import");
    assertTrue(r.terms().contains("content:import"));
  }

  @Test
  void quotedPhraseParsed() {
    SearchRequest r = parser.parse("\"hello world\"");
    assertTrue(r.terms().contains("\"hello world\""));
  }

  @Test
  void booleanOperatorsPreserved() {
    SearchRequest r = parser.parse("foo AND bar");
    assertTrue(r.terms().contains("AND"));
    assertTrue(r.terms().contains("foo"));
    assertTrue(r.terms().contains("bar"));
  }

  @Test
  void emptyInputReturnsEmptyTerms() {
    SearchRequest r = parser.parse("  ");
    assertTrue(r.terms().isBlank());
    assertTrue(r.extensions().isEmpty());
    assertTrue(r.directories().isEmpty());
  }

  @Test
  void extensionsAreLowercased() {
    SearchRequest r = parser.parse("ext:JAVA");
    assertEquals("java", r.extensions().get(0));
  }

  @Test
  void wildcardTokenPreserved() {
    SearchRequest r = parser.parse("Search*");
    assertTrue(r.terms().contains("Search*"));
  }

  @Test
  void mixedQualifiersAndTerms() {
    SearchRequest r = parser.parse("import ext:java path:src content:void");
    assertTrue(r.terms().contains("import"));
    assertTrue(r.extensions().contains("java"));
    assertFalse(r.directories().isEmpty());
    assertTrue(r.terms().contains("content:void"));
  }
}
