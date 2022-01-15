package io.github.douira.glsl_transformer;

import org.junit.jupiter.api.BeforeEach;

import io.github.douira.glsl_transformer.TestResourceManager.FileLocation;
import io.github.douira.glsl_transformer.transform.Transformation;
import io.github.douira.glsl_transformer.transform.TransformationManager;
import io.github.douira.glsl_transformer.transform.TransformationPhase;

/**
 * Handles setup of all the things required to run a transformation.
 */
public abstract class TestWithTransformationManager {
  private static String testResourceInput;

  private static String testCode;
  protected TransformationManager manager;

  public static void loadResource(FileLocation location) {
    testResourceInput = TestResourceManager.getResource(location).content();
  }

  public static void setTestCode(String code) {
    testCode = code;
  }

  @BeforeEach
  public void setupParsing() {
    setTestCode(testResourceInput);
  }

  public String runTransformation(String code, TransformationPhase phase) {
    manager = new TransformationManager();
    manager.registerTransformation(new Transformation(phase));
    return manager.transform(code);
  }

  public String runTransformation(TransformationPhase phase) {
    return runTransformation(testCode, phase);
  }
}
