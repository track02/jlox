package dev.plasticzen.lox;

import java.util.List;

/**
 * Expr is a syntax tree which is used to represent the syntax of lox
 * A visitor interface is supplied to allow easy addition of new functionality to the various expressions
 */
abstract class Expr {
    interface Visitor<R> {
    R visitBinaryExpr(Binary expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitUnaryExpr(Unary expr);
   }

  /**
   * To be implemented by each expression, calls the appropriate visit method on a supplied visitor
   * @param visitor Visitor to call visit method on
   * @return Visitor dependant
   * @param <R> Visitor dependant
   */
  abstract <R> R accept(Visitor<R> visitor);

  /**
   * Represents an expression consisting of two expressions and an operator
   * E.g. 10 + 100
   */
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  /**
   * Represents an expression within parentheses
   */
  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;
  }

  /**
   * Represents a literal, syntax which produces a specific value
   * E.g. 12
   */
  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;
  }

  /**
   * Represents an operand and sub-expression
   * E.g. -100
   */
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }
}
