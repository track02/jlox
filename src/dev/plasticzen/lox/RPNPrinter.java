package dev.plasticzen.lox;

public class RPNPrinter implements Expr.Visitor<String>{

    public String print(Expr expr){
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {

        return expr.left.accept(this) + " " + expr.right.accept(this) + " " + expr.operator.lexeme;
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {

        return expr.accept(this);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return null;
    }

    public static void main(String[] args){

        // 123 * 45.67
        // ->
        // 123 45.67 *

        Expr expression = new Expr.Binary(
                new Expr.Literal(123),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Literal(45.67));

        System.out.println(new RPNPrinter().print(expression));
    }
}