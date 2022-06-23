package io.github.douira.glsl_transformer.ast.node.expression;

import io.github.douira.glsl_transformer.ast.InnerASTNode;
import io.github.douira.glsl_transformer.ast.traversal.*;

public abstract class Expression extends InnerASTNode {
  public enum ExpressionType {
    REFERENCE, // TODO identifier
    LITERAL, // TODO literal
    GROUPING, // unary
    ARRAY_ACCESS, // TODO binary
    METHOD_CALL, // TODO (incomplete) unary + method + parameters
    FUNCTION_CALL, // TODO (incomplete) unary + parameters
    MEMBER_ACCESS, // unary + identifier
    INCREMENT_POSTFIX, // unary
    DECREMENT_POSTFIX, // unary
    INCREMENT_PREFIX, // unary
    DECREMENT_PREFIX, // unary
    IDENTITY, // unary
    NEGATION, // unary
    BOOLEAN_NOT, // unary
    BITWISE_NOT, // unary
    MULTIPLICATION, // TODO binary
    DIVISION, // TODO binary
    MODULO, // TODO binary
    ADDITION, // TODO binary
    SUBTRACTION, // TODO binary
    SHIFT_LEFT, // TODO binary
    SHIFT_RIGHT, // TODO binary
    LESS_THAN, // TODO binary
    GREATER_THAN, // TODO binary
    LESS_THAN_EQUAL, // TODO binary
    GREATER_THAN_EQUAL, // TODO binary
    EQUAL, // TODO binary
    NOT_EQUAL, // TODO binary
    BITWISE_AND, // TODO binary
    BITWISE_XOR, // TODO binary
    BITWISE_OR, // TODO binary
    BOOLEAN_AND, // TODO binary
    BOOLEAN_XOR, // TODO binary
    BOOLEAN_OR, // TODO binary
    CONDITION, // ternary
    ASSIGNMENT, // TODO binary
    MULTIPLICATION_ASSIGNMENT, // TODO binary
    DIVISION_ASSIGNMENT, // TODO binary
    MODULO_ASSIGNMENT, // TODO binary
    ADDITION_ASSIGNMENT, // TODO binary
    SUBTRACTION_ASSIGNMENT, // TODO binary
    LEFT_SHIFT_ASSIGNMENT, // TODO binary
    RIGHT_SHIFT_ASSIGNMENT, // TODO binary
    BITWISE_AND_ASSIGNMENT, // TODO binary
    BITWISE_XOR_ASSIGNMENT, // TODO binary
    BITWISE_OR_ASSIGNMENT, // TODO binary
    SEQUENCE // many-ary
  }

  public abstract ExpressionType getExpressionType();

  public enum OperandStructure {
    NONE,
    UNARY,
    BINARY,
    TERNARY,
    MANY
  }

  public abstract OperandStructure getOperandStructure();

  public abstract <R> R expressionAccept(ASTVisitor<R> visitor);

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitExpression(this);
  }

  @Override
  public void enterNode(ASTListener listener) {
    listener.enterExpression(this);
  }

  @Override
  public void exitNode(ASTListener listener) {
    listener.exitExpression(this);
  }
}
