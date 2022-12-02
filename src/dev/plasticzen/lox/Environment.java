package dev.plasticzen.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an environment, the location
 * in which names (i.e. variables) are bound to values
 */

public class Environment
{
    private final Map<String, Object> values = new HashMap<>();


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
     * Used to looking the value bound to a name
     * If the name is found the value is returned
     * Otherwise a RunTime error is thrown
     * @param name - binding name to lookup
     * @return - binding value
     */
    Object get(Token name){
        if (values.containsKey(name.lexeme)){
            return values.get(name.lexeme);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }


}
