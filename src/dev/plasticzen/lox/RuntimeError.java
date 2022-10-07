package dev.plasticzen.lox;


// Any errors encountered during interpreter evaluation
// are classed as Runtime errors
//
// Lox will implement its own system for error handling to
// better hide the underlying java implementation


class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}