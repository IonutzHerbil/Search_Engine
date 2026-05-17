package app.search.preprocessor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryPipelineTest {

  private QueryPreProcessor pipeline;

  @BeforeEach
  void setUp() {
    pipeline =
        new LogicDecorator(
            new SynonymDecorator(new SanitizationDecorator(raw -> raw == null ? "" : raw.trim())));
  }

  @Test
  void sanitization_stripsExclamationMarks() {
    String result = pipeline.process("login!!");
    assertFalse(result.contains("!"));
  }

  @Test
  void sanitization_stripsHashAndAtSymbols() {
    String result = pipeline.process("user#name @test");
    assertFalse(result.contains("#"));
    assertFalse(result.contains("@"));
  }

  @Test
  void sanitization_preservesHyphens() {
    String result = pipeline.process("well-known");
    assertTrue(result.contains("well-known"));
  }

  @Test
  void sanitization_collapsesMultipleSpaces() {
    String result = pipeline.process("login    logout");
    assertFalse(result.contains("  "));
  }

  @Test
  void sanitization_handlesNullInput() {
    assertDoesNotThrow(() -> pipeline.process(null));
    assertEquals("", pipeline.process(null));
  }

  @Test
  void sanitization_handlesBlankInput() {
    assertEquals("", pipeline.process("   "));
  }

  @Test
  void sanitization_doesNotStripQualifiers() {
    String result = pipeline.process("login ext:java path:src color:red");
    assertTrue(result.contains("ext:java"));
    assertTrue(result.contains("path:src"));
    assertTrue(result.contains("color:red"));
  }

  @Test
  void sanitization_doesNotStripQuotedPhrases() {
    String result = pipeline.process("\"hello world\"");
    assertTrue(result.contains("\"hello world\""));
  }

  @Test
  void logic_addsWildcardToPlainTerm() {
    assertTrue(pipeline.process("search").contains("search*"));
  }

  @Test
  void logic_doesNotDoubleWildcard() {
    String result = pipeline.process("search*");
    assertFalse(result.contains("search**"));
    assertTrue(result.contains("search*"));
  }

  @Test
  void logic_doesNotWildcardAndOperator() {
    String result = pipeline.process("login AND logout");
    assertTrue(result.contains("AND"));
    assertFalse(result.contains("AND*"));
  }

  @Test
  void logic_doesNotWildcardOrOperator() {
    String result = pipeline.process("login OR logout");
    assertTrue(result.contains("OR"));
    assertFalse(result.contains("OR*"));
  }

  @Test
  void logic_doesNotWildcardNotOperator() {
    String result = pipeline.process("login NOT admin");
    assertTrue(result.contains("NOT"));
    assertFalse(result.contains("NOT*"));
  }

  @Test
  void logic_doesNotWildcardExtQualifier() {
    String result = pipeline.process("login ext:java");
    assertTrue(result.contains("ext:java"));
    assertFalse(result.contains("ext:java*"));
  }

  @Test
  void logic_doesNotWildcardPathQualifier() {
    String result = pipeline.process("login path:src");
    assertTrue(result.contains("path:src"));
    assertFalse(result.contains("path:src*"));
  }

  @Test
  void logic_doesNotWildcardColorQualifier() {
    String result = pipeline.process("color:red");
    assertTrue(result.contains("color:red"));
    assertFalse(result.contains("color:red*"));
  }

  @Test
  void logic_doesNotWildcardQuotedPhrase() {
    String result = pipeline.process("\"hello world\"");
    assertFalse(result.contains("\"hello world\"*"));
  }

  @Test
  void logic_wildcardsMultipleTerms() {
    String result = pipeline.process("login logout admin");
    assertTrue(result.contains("login*"));
    assertTrue(result.contains("logout*"));
    assertTrue(result.contains("admin*"));
  }

  @Test
  void synonym_expandsImg() {
    String result = pipeline.process("img");
    assertTrue(result.contains("image"));
    assertTrue(result.contains("photo"));
  }

  @Test
  void synonym_expandsDoc() {
    String result = pipeline.process("doc");
    assertTrue(result.contains("document"));
  }

  @Test
  void synonym_expandsLog() {
    String result = pipeline.process("log");
    assertTrue(result.contains("logs"));
    assertTrue(result.contains("logging"));
  }

  @Test
  void synonym_expandsAuth() {
    String result = pipeline.process("auth");
    assertTrue(result.contains("authentication"));
    assertTrue(result.contains("authorization"));
  }

  @Test
  void synonym_unknownTermNotExpanded() {
    String result = pipeline.process("controller");
    assertTrue(result.contains("controller"));
    assertFalse(result.contains("OR"));
  }

  @Test
  void synonym_doesNotExpandQualifiers() {
    String result = pipeline.process("ext:img");
    assertTrue(result.contains("ext:img"));
    assertFalse(result.contains("ext:(img OR image"));
  }

  @Test
  void synonym_expandedGroupGetsWildcarded() {
    String result = pipeline.process("img");
    assertTrue(result.contains("img*"));
    assertTrue(result.contains("image*"));
    assertTrue(result.contains("photo*"));
  }

  @Test
  void pipeline_sanitizesThenExpandsThenWildcards() {
    String result = pipeline.process("img!!");
    assertFalse(result.contains("!"));
    assertTrue(result.contains("image*"));
    assertTrue(result.contains("photo*"));
  }

  @Test
  void pipeline_mixedQualifiersAndTerms() {
    String result = pipeline.process("login ext:java path:src");
    assertTrue(result.contains("login*"));
    assertTrue(result.contains("ext:java"));
    assertTrue(result.contains("path:src"));
    assertFalse(result.contains("ext:java*"));
    assertFalse(result.contains("path:src*"));
  }

  @Test
  void pipeline_booleanWithTerms() {
    String result = pipeline.process("login AND logout");
    assertTrue(result.contains("login*"));
    assertTrue(result.contains("logout*"));
    assertTrue(result.contains("AND"));
    assertFalse(result.contains("AND*"));
  }

  @Test
  void pipeline_emptyStringReturnsEmpty() {
    assertEquals("", pipeline.process(""));
  }

  @Test
  void pipeline_onlyQualifiersNoPlainTerms() {
    String result = pipeline.process("ext:java color:red");
    assertTrue(result.contains("ext:java"));
    assertTrue(result.contains("color:red"));
    assertFalse(result.contains("*"));
  }
}
