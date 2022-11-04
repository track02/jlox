package dev.plasticzen.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

    private static final Interpreter interpreter = new Interpreter();


    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    /**
     * Main entry point, checks passed in command line args and hands over to relevant function
     * @param args - command line arguments
     * @throws IOException - thrown on error with input file
     */
    public static void main(String[] args) throws IOException {

        if (args.length > 1) {  // Expect either a filename (run file) or nothing (run prompt)
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    // Opens file at given path and passes contents to run

    /**
     * Opens (lox) file at given path, reads in contents and passes to run
     * @param path - file path of lox script
     * @throws IOException - thrown on error with file
     */
    private static void runFile(String path) throws IOException {

        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(76);

    }

    // Reads in line by line from prompt and passes to run
    // Control-D used to quit -> signals 'end of file' which readLine returns as nul

    /**
     * Interactive prompt, reads in line-by-line from user and hands off to run
     * Control-D used to quit, signals 'end of file' which readLine returns as null
     * @throws IOException - thrown on error with reader
     */
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    /**
     * Takes in a string of source code and groups into tokens
     * @param source - source code string
     */
    private static void run(String source){
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();

        // Stop if error encountered during scanning / parsing
        if (hadError) return;

        // Run our parsed expression through the interpreter for execution
        interpreter.interpret(expression);



    }

    /**
     * Used to report an error back to user
     * @param line - line error occurred
     * @param message - error message
     */
    static void error(int line, String message){
        report(line, "", message);
    }

    /**
     * Helper function used to display errors and other messages to the user
     * @param line - line of lox generating the message  (E.g. Line 15)
     * @param where - code snippet generating the message (E.g. function(first, second,);
     * @param message - message to be displayed (E.g. Unexpected "," in arguments list)
     */
    private static void report(int line, String where, String message){
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    /**
     * Helper function to display a runtime error back to the user
     * to be called by interpreter if error is encountered during
     * expression evaluation
     * @param error - runtime error (caught by interpreter)
     */

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    static void error(Token token, String message){
        if (token.type == TokenType.EOF){
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }
}
