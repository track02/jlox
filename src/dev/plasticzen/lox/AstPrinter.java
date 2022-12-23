package dev.plasticzen.lox;


/**
 * A Printer which implements the visitor interface
 * Used to pretty print a given expression via print method
 */
public class AstPrinter implements Expr.Visitor<String> {

    /**
     * Calls the accept method on a given expression and passes a reference to self
     * upon which appropriate visit method is called
     * @param expr expression to print
     * @return formatted string representation of expression
     */
    String print(Expr expr){
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return null;
    }

    /**
     * Prints a binary expression
     * @param expr Binary Expression
     * @return Formatted string
     */
    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    /**
     * Prints a grouping expression
     * @param expr Grouping Expression
     * @return Formatted string
     */
    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    /**
     * Prints a literal expression
     * @param expr Literal expression
     * @return Formatted string
     */
    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    /**
     * Prints a Unary Expression
     * @param expr Unary Expression
     * @return Formatted String
     */
    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    /**
     * Prints a variable expression
     * @param expr
     * @return
     */
    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return parenthesize(expr.name.lexeme);
    }


    /**
     * Formats a given expression into a parenthesised string
     * @param name Expression name
     * @param exprs Expression(s)
     * @return Formatted string
     */
    private String parenthesize(String name, Expr... exprs){
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs){
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    public static void main(String[] args){
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)));

        System.out.println(new AstPrinter().print(expression));
    }
}
