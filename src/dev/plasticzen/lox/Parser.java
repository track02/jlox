package dev.plasticzen.lox;

import java.util.List;
import static dev.plasticzen.lox.TokenType.*;

public class Parser {

    private static class ParseError extends RuntimeException {}

    /*
     * Where the scanner converted raw source code into tokens
     * Our parser will convert tokens into syntax trees, which
     * represent the various expressions in lox as defined
     * by rules in syntactic grammar
     */

    private final List<Token> tokens;
    private int current;

    Parser(List<Token> tokens){
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }


    // Each method for parsing a grammar rule produces a syntax tree for that
    // rule and returns it

    // When a rule body contains a non-terminal (ref. to another rule) we
    // call that other rule's method

    // expression -> equality
    private Expr expression(){
        return equality();
    }

    // equality -> comparison ( ( "!=" | "==" ) comparison )*
    private Expr equality(){

        // First comparison nonterminal
        Expr expr = comparison();

        // Maps to (...)*
        // Repeat until no more != or == are found
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;

    }

    // comparison -> term ( (">" | ">=" | "<" | "<=" ) term)*
    private Expr comparison(){
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // term -> factor ( ( "-" | "+" ) factor )*
    private Expr term(){
        Expr expr = factor();

        while (match(MINUS, PLUS)){
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // factor -> unary ( ("/" | "*") unary )*
    private Expr factor(){
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // unary -> ( "!" | "-" ) unary
    // Look at current token to see how to parse it, if ! or - we must have a unary
    // expression, so take the token and recursively call unary to parse the operand
    // and wrap up return result into Unary Expression
    // Otherwise we have reached highest level of precedence, primary expressions
    private Expr unary(){
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    // primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
    private Expr primary(){
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)){
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }



    // Checks to see if current token has any of the given types
    // If it does match token is consume and pointer advances
    // Note - ... next to type indicates var args, zero or more TokenTypes to be passed
    private boolean match(TokenType... types){
        for (TokenType type : types){
            if (check(type)){
                advance();
                return true;
            }
        }
        return false;
    }

    // Similar to match, checks to see if next token is of expected type
    // If so it consumes the token, if not then raise an error
    private Token consume(TokenType type, String message){
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    // Check looks at current token and compares its type
    // Peek is used to avoid advancing current token pointer
    private boolean check(TokenType type){
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    // Consumes current token and returns it, similar to how scanner operates
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    // Checks if tokens still exist to be parsed
    private boolean isAtEnd(){
        return peek().type == EOF;
    }

    // Returns current token we have yet to consume
    private Token peek(){
        return tokens.get(current);
    }

    // Returns most recently consumed token
    private Token previous(){
        return tokens.get(current -1);
    }

    private ParseError error(Token token, String message){
        Lox.error(token, message);
        return new ParseError();
    }

    // Synchronise is used to advance parsers state until at the beginning
    // of the next statement, the boundary between statements is marked by a semicolon
    // Most statements begin with a keyword (for, if, return ...) when the next token
    // is any of these we know we're at the beginning of a statement
    //
    // So discard tokens until a statement boundary is found
    // This can be used to re-sync the parser following a parse error, discard the tokens
    // that are part of the error-causing statement and start again at the next
    private void synchronize(){
        advance();

        while (!isAtEnd()) {

            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();

        }

    }

}
