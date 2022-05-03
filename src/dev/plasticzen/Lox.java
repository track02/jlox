package dev.plasticzen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Lox {

    /**
     * Chp 3 - Scanner, Starting point is a scanner which takes in raw source code and groups it into a series of
     * tokens  are the meaningful elements that make up the languages grammar
     **/

    // Main entry point, handles check of command line args and handover to relevant function
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
    private static void runFile(String path) throws IOException {

        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

    }

    // Reads in line by line from prompt and passes to run
    // Control-D used to quit -> signals 'end of file' which readLine returns as null
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
        }
    }

    private static void run(String source){
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        for (Token token : tokens) {
            System.out.println(token);
        }

    }
}
