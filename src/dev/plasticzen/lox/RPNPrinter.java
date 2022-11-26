package dev.plasticzen.lox;

/*
 Additional task - add an RPN printer using the visitor pattern
 */
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
        return expr.operator.lexeme + expr.right.accept(this);
    }


    public static void main(String[] args){

        // -123 * 45.67
        // ->
        // -123 45.67 *

        Expr expression = new Expr.Binary(
                new Expr.Binary(
                        new Expr.Literal(1),
                        new Token(TokenType.PLUS, "+", null, 1),
                        new Expr.Literal(2)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Binary(
                        new Expr.Literal(4),
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(3)));

        System.out.println(new RPNPrinter().print(expression));
    }
}
