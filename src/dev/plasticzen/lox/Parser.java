package dev.plasticzen.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static dev.plasticzen.lox.TokenType.*;

/*
 * Where the scanner converted raw source code into tokens
 * Our parser will convert tokens into syntax trees, which
 * represent the various expressions in lox as defined
 * by rules in syntactic grammar
 */


public class Parser {

    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens; // Tokens from scanner to convert into expressions
    private int current;

    /*
     * A lox script consists of number of declarations followed by an end of file
     * Statements can either be expression statements or print statements
     * An expression statement is an expression followed by a semicolon
     * A print statement is an expression preceded by 'print'
     *
     */

    /*
    program        → declaration* EOF ;

    declaration    → varDecl
                   | statement ;

    statement      → exprStmt
                   | printStmt
                   | block;


    block          → "{" declaration* "}" ;

    varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
    exprStmt       → expression ";" ;
    printStmt      → "print" expression ";" ;
    expression     → assignment ;
    assignment     → IDENTIFIER "=" assignment
                   | equality ;
                   | logic_or ;

    logic_or       → logic_and ( "or" logic_and )* ;
    logic_and      → equality ( "and" equality )* ;

    equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    term           → factor ( ( "-" | "+" ) factor )* ;
    factor         → unary ( ( "/" | "*" ) unary )* ;
    unary          → ( "!" | "-" ) unary
                   | primary ;
    primary        → "true" | "false" | "nil"
                   | NUMBER | STRING
                   | "(" expression ")"
                   | IDENTIFIER ;

     */

    Parser(List<Token> tokens){
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }


    // Each method for parsing a grammar rule produces a syntax tree for that
    // rule and returns it

    // When a rule body contains a non-terminal (ref. to another rule) we
    // call that other rule's method

    // expression -> equality
    private Expr expression(){
        return assignment();
    }

    /**
     * Declaration is called repeatedly when parsing a series of statements
     * It first checks for a variable declaration by looking for a VAR keyword, if not present
     * it falls through to the statement method which parses print and expression statements
     * @return Parsed statement
     */
    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /**
     * A program consists of a list of statements, this method is used
     * to parse out one statement at a time
     * @return Stmt parsed from current position
     */
    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(WHILE)) return whileStatement();

        return expressionStatement();
    }

    /**
     * Parses a for statement
     * Desugars into while statement
     * @return For Statement
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)){
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");


        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;

    }

    /**
     * Parses an if statement
     * @return If Statement
     */
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }



    /**
     * Parses a print statement
     * @return Print Stmt
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }


    /**
     * Parses a var statement from tokens
     * Expect parser to have already matched `var` so checks
     * first for identifier (variable name) then looks for =
     * If present knows there is an initialiser expression, if not
     * leaves as null
     * Lastly consumes required semicolon at end of statement and
     * wraps up tokens as Stmt.var syntax tree
     *
     * Note - This is for variable declaration, (var x = ...)
     * which differs from assignment (x = ...)
     *
     * @return Var statement
     */
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name");

        Expr initializer = null;
        if (match(EQUAL)){
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    /***
     * Parses a while statement from tokens
     * Expect While to already have been matched
     * So pulls out the conditional within the brackets
     * And then the following statement to be executed
     * @return While statement
     */
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    /**
     * Parses an expression followed by a semicolon (expression statement)
     * @return Parsed expression statement
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    /**
     * Parses a block of statement by matching on braces
     * @return a list of statement contained inside matched block
     */
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * Parses an assignment or drops through to equality
     * @return Expr
     */
    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    /**
     * Parses a logical or operation
     * logic_or       → logic_and ( "or" logic_and )* ;
     * @return Logical expression
     */
    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parses a logical and operation
     logic_and  → equality ( "and" equality )* ;
     * @return Logical expression
     */
    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }


    /**
     * equality -> comparison ( ( "!=" | "==" ) comparison )*
     * @return Expr representing equality comparison
     */
    private Expr equality(){

        // First comparison non-terminal
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

    /**
     * comparison -> term ( (">" | ">=" | "<" | "<=" ) term)
     * @return Expr representing comparison
     */
    private Expr comparison(){
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * term -> factor ( ( "-" | "+" ) factor )
     * @return Expr, representing term
     */
    private Expr term(){
        Expr expr = factor();

        while (match(MINUS, PLUS)){
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * factor -> unary ( ("/" | "*") unary )
     * @return Expr, representing factor
     */

    private Expr factor(){
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }


    /**
     * unary -> ( "!" | "-" ) unary
     * Examines current token to see how to parse it, if ! or - we must have a unary
     * expression, so take the token and recursively call unary to parse the operand
     * and wrap up return result into Unary Expression
     * Otherwise we have reached highest level of precedence, call expressions
     * @return Expr, representing Unary
     */
    private Expr unary(){
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    /**
     * Advances parser collecting argument for given callee
     * until ) is reached, bundles together and returns a Call expression
     * @param callee - expression to apply arguments
     * @return call expression
     */
    private Expr finishCall(Expr callee){
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)){
            do {
                if (arguments.size() >= 255){
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    /**
     * Parses a function call, by first matching a primary expression
     * and if an opening parentheses is found finishes off the call by extracting arguments
     * and returning a Call
     * Otherwise returns just the primary
     * @return Expr representing a call / primary
     */
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;


    }

    /**
     * primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER
     * @return Expr, representing Primary
     */
    private Expr primary(){
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)){
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)){
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    /**
     * Checks to see if current token has any of the given types
     * If it does match token is consumed and pointer advances
     * Note - ... next to type indicates var args, zero or more TokenTypes to be passed
     */
    private boolean match(TokenType... types){
        for (TokenType type : types){
            if (check(type)){
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Similar to match, checks to see if next token is of expected type
     * If so it consumes the token, if not then raise an error
     */
    private Token consume(TokenType type, String message){
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    /**
     * Check looks at current token and compares its type
     * Peek is used to avoid advancing current token pointer
     */
    private boolean check(TokenType type){
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /**
     * Consumes current token and returns it, similar to how scanner operates
     * @return current token
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    /**
     * Checks if tokens still exist to be parsed
     * @return bool, indicating end of token list
     */
    private boolean isAtEnd(){
        return peek().type == EOF;
    }

    /**
     * Returns current token we have yet to consume
     * @return current token
     */
    private Token peek(){
        return tokens.get(current);
    }

    /**
     * Returns most recently consumed token
     * @return last consumed token
     */
    private Token previous(){
        return tokens.get(current -1);
    }

    /**
     *  Reports a lox error, pairing an error message for a given token
     * @param token token which caused error
     * @param message error message
     * @return ParseError
     */
    private ParseError error(Token token, String message){
        Lox.error(token, message);
        return new ParseError();
    }

    /**
     * Synchronise is used to advance parsers state until at the beginning
     * of the next statement, the boundary between statements is marked by a semicolon
     * Most statements begin with a keyword (for, if, return ...) when the next token
     * is any of these we know we're at the beginning of a statement
     *
     * So discard tokens until a statement boundary is found
     * This can be used to re-sync the parser following a parse error, discard the tokens
     * that are part of the error-causing statement and start again at the next
     */
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
