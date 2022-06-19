package io.github.douira.glsl_transformer.ast.print;

import io.github.douira.glsl_transformer.GLSLLexer;
import io.github.douira.glsl_transformer.ast.node.*;
import io.github.douira.glsl_transformer.ast.node.external_declaration.*;
import io.github.douira.glsl_transformer.ast.node.external_declaration.PragmaStatement.PragmaType;
import io.github.douira.glsl_transformer.ast.node.statement.*;
import io.github.douira.glsl_transformer.ast.print.token.EOFToken;

public abstract class ASTPrinter extends ASTPrinterUtil {
  @Override
  public void exitTranslationUnit(TranslationUnit node) {
    emitToken(new EOFToken(node));
  }

  @Override
  public Void visitVersionStatement(VersionStatement node) {
    emitType(node, GLSLLexer.NR, GLSLLexer.VERSION);
    emitSpace(node);
    emitLiteral(node, Integer.toString(node.version));
    if (node.profile != null) {
      emitSpace(node);
      emitType(node, node.profile.tokenType);
    }
    emitNewline(node);
    return null;
  }

  @Override
  public Void visitEmptyDeclaration(EmptyDeclaration node) {
    emitType(node, GLSLLexer.SEMICOLON);
    emitNewline(node); // optional
    return null;
  }

  @Override
  public Void visitPragmaStatement(PragmaStatement node) {
    emitType(node, GLSLLexer.NR, GLSLLexer.PRAGMA);
    emitSpace(node);
    if (node.stdGL) {
      emitType(node, GLSLLexer.NR_STDGL);
      emitSpace(node);
    }
    if (node.type == PragmaType.CUSTOM) {
      emitLiteral(node, node.customName);
    } else {
      emitType(node,
          node.type.tokenType,
          GLSLLexer.NR_LPAREN,
          node.state.tokenType,
          GLSLLexer.NR_RPAREN);
    }
    emitNewline(node);
    return null;
  }

  @Override
  public Void visitExtensionStatement(ExtensionStatement node) {
    emitType(node, GLSLLexer.NR, GLSLLexer.EXTENSION);
    emitSpace(node);
    emitLiteral(node, node.name);
    emitType(node, GLSLLexer.NR_COLON);
    emitSpace(node);
    emitType(node, node.behavior.tokenType);
    emitNewline(node);
    return null;
  }

  @Override
  public void exitLayoutDefaults(LayoutDefaults node) {
    emitType(node, node.mode.tokenType);
    emitSpace(node);
    emitType(node, GLSLLexer.SEMICOLON);
    emitNewline(node); // optional
  }

  @Override
  public Void visitEmptyStatement(EmptyStatement node) {
    emitType(node, GLSLLexer.SEMICOLON);
    emitNewline(node); // optional
    return null;
  }

  @Override
  public void enterCompoundStatement(CompoundStatement node) {
    emitType(node, GLSLLexer.LBRACE);
    emitNewline(node); // optional
  }

  @Override
  public void exitCompoundStatement(CompoundStatement node) {
    emitType(node, GLSLLexer.RBRACE);
    emitNewline(node); // optional
  }

  @Override
  public Void visitIdentifier(Identifier node) {
    emitLiteral(node, node.name);
    return null;
  }
}
