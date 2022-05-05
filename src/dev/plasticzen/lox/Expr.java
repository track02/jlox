package dev.plasticzen.lox;

import java.util.List;

abstract class Expr {
  static class Binary extends Expr {
    Binary(Expr left, Token operation, Expr right) {
      this.left = left;
      this.operation = operation;
      this.right = right;
    }

    final Expr left;
    final Token operation;
    final Expr right;
  }
  static class Grouping extends Expr {
    Grouping(Expr expressions) {
      this.expressions = expressions;
    }

    final Expr expressions;
  }
  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    final Object value;
  }
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    final Token operator;
    final Expr right;
  }
}
