package dev.plasticzen.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an environment, the location
 * in which names (i.e. variables) are bound to values
 */

public class Environment
{

    final Environment enclosing; // Reference to environment enclosing this one

    private final Map<String, Object> values = new HashMap<>();

    Environment() { // Top level 'global' environment
        enclosing = null;
    }
    Environment(Environment enclosing) { // Enclosed environments
        this.enclosing = enclosing;
    }


    /**
     * Used to define a new binding
     * bines the given name to the given value
     * @param name - binding name
     * @param value - binding value
     */
    void define(String name, Object value){
        values.put(name, value);
    }

    /**
     * Used to locate the value bound to a name
     * by checking current environment along with any parent environments
     * If the name is found the value is returned
     * Otherwise a RunTime error is thrown
     * @param name - binding name to lookup
     * @return - binding value
     */
    Object get(Token name){
        if (values.containsKey(name.lexeme)){
            return values.get(name.lexeme);
        }

        if (enclosing != null) return enclosing.get(name); // Walk up the chain of nested environments searching for variable

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    /**
     * Updates the value of an existing variable
     * in the environment
     * @param name variable name
     * @param value variable value
     */
    void assign(Token name, Object value) {

        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }



}
