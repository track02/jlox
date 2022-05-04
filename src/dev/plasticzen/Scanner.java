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
            default -> Lox.error(line, "Unexpected Character");
        }
    }

    /**
     * Advances through the source and returns next character
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
