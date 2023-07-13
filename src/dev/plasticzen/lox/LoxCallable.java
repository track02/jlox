package dev.plasticzen.lox;

import java.util.List;

/***
 * Represents any Lox object that called like a function
 * I.E user defined functions and class objects
 */
interface LoxCallable {
    Object call(Interpreter interpreter, List<Object> arguments);
}