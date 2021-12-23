package io.github.douira.glsl_transformer.transform;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.annotations.SnapshotName;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import io.github.douira.glsl_transformer.GLSLParser;
import io.github.douira.glsl_transformer.GLSLParser.TranslationUnitContext;
import io.github.douira.glsl_transformer.IntegratedTest;
import io.github.douira.glsl_transformer.TestCaseProvider;
import io.github.douira.glsl_transformer.generic.PrintVisitor;
import io.github.douira.glsl_transformer.transform.TransformationPhase.InjectionPoint;

@ExtendWith({ SnapshotExtension.class })
public class TransformationPhaseTest extends IntegratedTest {
  private Expect expect;

  class EmptyPhase extends TransformationPhase {
  }

  @BeforeAll
  static void setupInput() {
    readInput(TransformationPhaseTest.class);
  }

  @Test
  void testCompilePath() {
    wrapRunTransform(new EmptyPhase() {
      @Override
      protected void init() {
        var path = compilePath("/translationUnit/externalDeclaration");
        assertEquals(
            path.evaluate(tree).size(), tree.getChildCount() - 2,
            "It should compile a functioning xpath");
      }
    });
  }

  @Test
  void testCompilePattern() {
    wrapRunTransform(new EmptyPhase() {
      @Override
      protected void init() {
        var pattern = compilePattern("varying <type:typeSpecifier> varyVec;",
            GLSLParser.RULE_externalDeclaration);
        var match = pattern.match(tree.getChild(5));
        assertTrue(match.succeeded(), "It should compile a functioning pattern");
        assertEquals(
            "vec2", match.get("type").getText(),
            "It should compile a functioning pattern");
      }
    });

  }

  @Test
  void testCreateLocalRoot() {

  }

  @Test
  void testFindAndMatch() {

  }

  @Test
  void testGetSiblings() {
    assertEquals(tree.children, TransformationPhase.getSiblings(tree.versionStatement()),
        "It should find the siblings of a node");
  }

  @Test
  void testInjectExternalDeclaration() {

  }

  /*
   * NOTE: #define is not a parsed directive and is disregarded
   * TODO: add snapshot tests for unparsed tokens like comments and #defines
   * TODO: found injection bugs in shapshot tests:
   * full_reverse/before_eof, single_extension/*:
   * missing newline after extension
   * 
   * single_pragma/*:
   * missing newline after pragma
   * 
   * single_version/*:
   * missing newline after version
   * 
   * full_reverse/before_directives, /before_declarations, /before_extensions:
   * duplicated version in injected string (issue with printer?)
   */
  @ParameterizedTest
  @ArgumentsSource(TestCaseProvider.class)
  @SnapshotName("testInjectNode")
  void testInjectNode(String scenario, String input) {
    for (var injectionPoint : InjectionPoint.values()) {
      setupParsingWith(input);
      wrapRunTransform(new RunPhase() {
        @Override
        protected void run(TranslationUnitContext ctx) {
          injectExternalDeclaration("injection;", injectionPoint);
        }
      });

      var output = PrintVisitor.printTree(tokenStream, tree);
      expect
          .scenario(scenario + "/" + injectionPoint.toString().toLowerCase())
          .toMatchSnapshot(input, "<>".repeat(25), output);
    }
  }

  @Test
  void testIsActive() {
    EmptyPhase phase = new EmptyPhase();
    assertTrue(phase.isActive(), "It should always be active");
  }

  @Test
  void testRemoveNode() {

  }

  @Test
  void testReplaceNode() {

  }

  @Test
  void testSetParent() {
    wrapRunTransform(new RunPhase() {
      @Override
      protected void run(TranslationUnitContext ctx) {
        assertEquals(parser, getParser(),
            "It should return the previously set parser inside the phase collector");
      }
    });
  }
}
