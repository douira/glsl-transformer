package io.github.douira.glsl_transformer.ast.node;

import java.util.List;

import io.github.douira.glsl_transformer.ast.*;

public class TranslationUnit extends ListASTNode<ExternalDeclaration> {
  public VersionStatement versionStatement;

  public TranslationUnit(VersionStatement versionStatement, List<ExternalDeclaration> externalDeclarations) {
    super(externalDeclarations);
    this.versionStatement = versionStatement;
  }

  @Override
  public void enterNode(ASTListener listener) {
    listener.enterTranslationUnit(this);
  }

  @Override
  public void exitNode(ASTListener listener) {
    listener.exitTranslationUnit(this);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitTranslationUnit(this);
  }

  public <R> R visitChildren(ASTVisitor<R> visitor) {
    var result = visitor.defaultResult();
    if (versionStatement != null) {
      result = visitor.visit(result, versionStatement);
    }
    for (var child : children) {
      result = visitor.visit(result, child);
    }
    return result;
  }
}
