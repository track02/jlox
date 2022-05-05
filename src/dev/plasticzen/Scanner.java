package dev.plasticzen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.plasticzen.TokenType.*;

/*
 * Scanner operates on a string of source code
 * working through from start to end and generating tokens
 * for each lexeme present
 */




public class Scanner {

    private final String source; // Raw source code
    private final List<Token> tokens = new ArrayList<>(); // Empty list of tokens, populated after scan

    // Keyword map
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }
    private int start = 0; // Index of first character in lexeme
    private int current = 0; // Index of current character being considered
    private int line = 1; // Tracks line of source code being scanned

    /**
     * Constructor
     * @param source - source code as string
     */
    Scanner(String source){
        this.source = source;
    }

    /**
     * Scans over source and outputs a list of tokens
     * @return List of tokens generated from source
     */
    List<Token> scanTokens(){
        while(!isAtEnd()){
            // We are at the beginning of the next lexeme
            start = current;
            scanToken();
        }

        // End of loop, add EOF
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    /**
     * Helper method - checks if at end of source string
     * @return None
     */
    private boolean isAtEnd(){
        return current >= source.length();
    }

    /**
     * Scans source code, finds next lexeme and generates token
     * Unrecognised characters result in error
     */
    private void scanToken() {
        char c = advance();
        switch (c) {
            // Single character lexemes
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case ',' -> addToken(COMMA);
            case '.' -> addToken(DOT);
            case '-' -> addToken(MINUS);
            case '+' -> addToken(PLUS);
            case ';' -> addToken(SEMICOLON);
            case '*' -> addToken(STAR);
            // '/' is a special case as it can be used for comments and division
            case '/' -> slash();
            // Second character lexemes, look at next character and check
            case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
            case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
            case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);
            // Skip over meaningless characters
            case ' ', '\r', '\t' -> {}
            // Increment line counter for a new line
            case '\n' -> line++;
            // String lexeme handling
            case '"' -> string();
            default -> {
                // Number lexemes
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                // Unrecognised characters
                    Lox.error(line, "Unexpected Character");
                }
            }
        }
    }

    /**
     * Repeatedly advances ahead until a complete identifier is found (alphanumeric lexeme)
     * and generates appropriate token
     */
    private void identifier(){
        while (isAlphaNumeric(peek())) advance();

        // Identifier has been scanned in, extract text
        String text = source.substring(start, current);

        // Check keywords if this identifier is reserved
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER; // If not a keyword, lookup returns null
        addToken(type);
    }

    /**
     * Repeatedly looks ahead until a complete number is found
     * either integer or fractional and generates appropriate token
     */
    private void number(){

        // Consumes as many digits that make up the integer part of number and advance current
        while (isDigit(peek())) advance();

        // Look for a fractional part, continue advancing only if a digit is *after* the decimal point
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume '.', advancing current
            advance();

            // Consume as many digits that make up the fractional part of number advancing current
            while (isDigit(peek())) advance();
        }

        // Current now points at the 'last' digit in the number, create a token
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Repeatedly looks ahead until terminating " is found
     * When end of string is found trims "'s, extracts value
     * and generates a new token
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()){
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()){
            Lox.error(line, "Unterminated string");
            return;
        }

        // The closing "
        advance();

        // Trim surrounding quotes
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    /**
     * Handles a slash character, determining if division operator, single line comment or multiline
     * Assumes current is at first character of comment
     * Handles nesting and new lines
     */
    private void slash(){

        // Comment - Single Line, continue advancing until end of the line
        if (match('/')) {
            while (peek() != '\n' && !isAtEnd()) advance();

        // Multiline Comment
        } else if (match('*')) {

            int open_comments = 1;
            System.out.println("[Open found] Total open comments: " + open_comments);

            char last_char = source.charAt(current);

            // Advance to next character and begin loop
            advance();

            while (open_comments > 0) {

                // Account for multi line comments
                if (peek() == '\n') {
                    line++;
                    System.out.println("[New Line in comment] Current line: " + line);
                }
                if (last_char == '/' && peek() == '*') {
                    open_comments++;
                    System.out.println("[Open found] Total open comments: " + open_comments);
                }
                if (last_char == '*' && peek() == '/') {
                    open_comments--;
                    System.out.println("[Close found] Total open comments: " + open_comments);
                }

                // Capture last character
                if (!isAtEnd()){
                    last_char = source.charAt(current);
                    advance();
                }
                else {Lox.error(line, "Unclosed comment"); break;}
            }
        } else {

        // Division operator
        addToken(SLASH);
        }
    }

    /**
     * Conditional advance, the current character is only consumed if it matches
     * with the expected character
     * @param expected - Expected character to match
     * @return - true if characters match otherwise false
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        // Don't advance unless it's a match
        current++;
        return true;
    }

    /**
     * Peek is a helper method, similar to advance but doesn't consume
     * the character - one character of lookahead
     * @return - the current character
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * peekNext returns the character *after* current
     * @return - the current following current
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    /**
     * Helper method which checks if a given character
     * is an alphabetical character (+ underscore)
     * @param c - character to check
     * @return - bool, true if alphabetical/underscore otherwise false
     */
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    /**
     * Helper method combining isAlpha and isDigit
     * to check for alphanumeric character
     * @param c - character to check
     * @return - bool, true if alphanumeric, otherwise false
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * Helper method which checks if given character is a number
     * @param c - character to check
     * @return - bool, true if digit false otherwise
     */
    private boolean isDigit(char c){
        return c >= '0' && c <= '9';
    }

    /**
     * Advances through the source and returns next character
     * Current is then increment in preparation for next advance
     * @return - Next character of source
     */
    private char advance(){
        return source.charAt(current++);
    }

    /**
     * Used to create non-literal tokens, passes to overloaded method with null literal
     * @param type - Token Type (enum)
     */
    private void addToken(TokenType type){
        addToken(type, null);
    }

    /**
     * Generates a token using start and current indexes to extract substring of source
     * @param type - Token type (enum)
     * @param literal - Literal value if appropriate
     */
    private void addToken(TokenType type, Object literal){
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

}
