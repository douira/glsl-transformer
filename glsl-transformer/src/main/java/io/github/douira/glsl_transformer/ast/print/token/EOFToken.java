package io.github.douira.glsl_transformer.ast.print.token;

import io.github.douira.glsl_transformer.GLSLParser;
import io.github.douira.glsl_transformer.print.filter.TokenChannel;

public class EOFToken extends ParserToken {
  public EOFToken() {
    super(TokenChannel.HIDDEN, GLSLParser.EOF);
  }

  @Override
  public String getContent() {
    return "";
  }
}
