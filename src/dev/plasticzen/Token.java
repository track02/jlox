package dev.plasticzen;

/*
 * Scanner produces tokens which bundle substrings of the source code
 * and other information to produce a token
 */
public class Token {
    final TokenType type; // Enum identifier
    final String lexeme; // Raw substring of source code
    final Object literal; // Conversion of text to actual object (for literal string / num)
    final int line; // Line in source this token was created

    /**
     * Token Constructor
     * @param type - enum identifier (TokenType)
     * @param lexeme - raw code string
     * @param literal - literal conversion of above (if relevant)
     * @param line - line in source code
     */
    Token(TokenType type, String lexeme, Object literal, int line){
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    /**
     * Overrides toString, for reporting
     * @return - String representation of token
     */
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }


}
