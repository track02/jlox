package dev.plasticzen.lox;

/*
  The interpreter takes the syntax tree generated by the parser and
  produces values from the expressions it consists of

  Lox values are created by literals, computer by expressions and stored in variables
  The user sees these as Lox 'objects' but in reality they are implemented in the underlying language
  the interpreter is written in, Java

  We can use Java's object type for our dynamic Lox values as it allows us to check the type of a value at runtime (instanceof)
  Java also provides boxed versions of its primitive types which subclass object, these can be used for Lox's built in types

  So:

  Lox Type              Java Representation
  Any Lox Value         Object
  nil                   null
  Boolean               Boolean
  number                Double
  string                String

  The Interpreter will use the Visitor pattern and upon visiting an expression will return an Object

  For executing statements the Interpreter must also implement the Statement Visitor
  As statements produce not return value the return type of visit methods is void

 */

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>,
                                    Stmt.Visitor<Void>{


    private Environment environment = new Environment();

    /**
     * Given a lox expression attempts to evaluate it and display the result
     * @param expression lox expression to evaluate
     */
    void interpret(List<Stmt> statements) {
        try{
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error){
            Lox.runtimeError(error);
        }
    }

    /**
     * Helper method used to send expression back into
     * interpeter visitor implementation allowing for
     * recursive evaluation of subexpressions
     * @param expr Expression to evaluate
     * @return Object value, result of evaluation
     */
    private Object evaluate(Expr expr){
        return expr.accept(this);
    }


    /**
     * Helper method used to pass interpreter to given statements accept method
     * Which calls type specific visit method
     * @param stmt - Statement to execute
     */
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    /**
     * Given a block (list of statements) and enclosing environment
     * Switches to the given environment and attempts to execute each statement in the block
     * @param statements Block of statements
     * @param environment Block environment
     */
    void executeBlock(List<Stmt> statements,
                      Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    /**
     * Visiting a block statement results in executing all statements within the block
     * under the current environment
     * @param stmt Block statement
     * @return null
     */
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    /**
     * For expression statements we evaluate the inner
     * expression using the existing evaluate method and then discard the value
     * Note - Java requires a null return to satisfy Void return type
     * @param stmt - Expression statement to execute
     * @return null
     */
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt){
        evaluate(stmt.expression);
        return null;
    }



    /**
     * Evaluates the if condition, if truthy then executes the then branch
     * Otherwise executes the else branch if present
     * @param stmt - If statement to execute
     * @return null
     */
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }


    /**
     * Print statement has a similar visit method
     * The inner expression is evaluated, the result is printed and then discarded
     * @param stmt - Print statement to execute
     * @return null
     */
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    /**
     * Evaluates a variable declaration statement and updates environment appropriately
     * If variable has an initializer (a = 10) then the initializer is evaluated and the value is
     * paired to the variable name under the environment
     * @param stmt variable statement
     * @return null
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt){
        Object value = null;
        if (stmt.initialiser != null){
            value = evaluate(stmt.initialiser);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    /**
     * Evaluates a while statement
     * Making use of underlying java while loop to repeatedly
     * execute the statement body whilst the condition is truthy
     * @param stmt - While statement to be executed
     * @return null
     */
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }


    /**
     * Evaluates a variable assignment and updates the environment accordingly
     * @param expr Assignment expression
     * @return  null
     */
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }


    /*
     * Literals form the leaves of our tree, the atomic bits of syntax which other expressions are made of
     * Note that Literals are values but *syntax* that produces a value
     */

    /**
     * Converts a literal expression into a runtime value
     * @param expr Literal expression
     * @return Object value
     */
    @Override
    public Object visitLiteralExpr(Expr.Literal expr){
        return expr.value;
    }


    /**
     * Converts a logical expression into a truthy value
     * @param expr Logical expression to evaluate
     * @return Object value 
     */
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }


    // A grouping contains a single reference to another expression
    // in this case we recursively evaluate the subexpression until a value is reached

    /**
     * Evaluates a grouping expression returning an object value
     * @param expr Grouping expression
     * @return Object value
     */
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr){
        return evaluate(expr.expression);
    }

    // Unary expressions also consist of a single subexpression
    //
    // First evaluate operand expression then apply the operator to the result of that evaluation
    // We can't evaluate an operator until the operand is evaluated, this means the interpreter is performing
    // a post-order traversal - each node evaluates its children before doing its own work

    @Override
    public Object visitUnaryExpr(Expr.Unary expr){
        Object right = evaluate(expr.right); // Operand

        switch (expr.operator.type) { // Operator application
            case MINUS:
                    // We need to check for errors during interpreter evaluation, confirm operands are numeric
                    checkNumberOperand(expr.operator, right);
                    return -(double) right; // A minus must mean the subexpression is a number, perform cast
            case BANG: // Logical not, determine whether operand is truthy and invert result (True <-> False)
                    return !isTruthy(right);
            default:
                    return null;
        }
    }

    /**
     * Evaluates a given variable expression by looking up the variable name in the environment and
     * returning the assigned value (if any)
     * @param expr variable expression to evaluate
     * @return variable value
     */
    @Override
    public Object visitVariableExpr(Expr.Variable expr){
        return environment.get(expr.name);
    }


    // Binary expressions cover a range of operations
    //
    // Note that addition is a special case and can evaluate
    // to either a number or a string depending on operand type

    /**
     * Evaluates a binary expression returning an object value
     * @param expr Binary expression
     * @return Object value
     */
    @Override
    public Object visitBinaryExpr(Expr.Binary expr){
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");

            case BANG_EQUAL: // !=
                return !isEqual(left,right);
            case EQUAL_EQUAL:
                return isEqual(left,right);


        }

        // Unreachable
        return null;
    }


    // We need to decide what happens with values other than 'True' or 'False' in a logic operation
    // Lox treats null and false as false and everything else is true

    /**
     * Determines whether a given object is truthful
     * null / false -> False
     * everything else -> True
     * @param object Object to check
     * @return boolean, object is or isn't truthful
     */
    private boolean isTruthy(Object object){
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    // Equality operators support operands of any type, we'll use
    // an isEqual method to figure out the equality
    // Lox handles equality in a similar way to Java and doesn't perform
    // implicit conversions
    //
    // null needs to be handled specifically to prevent a NullPointerException
    //
    // Everything else can be handled via Java's equals method

    /**
     * Determines equality of two lox objects
     * @param a Object A
     * @param b Object B
     * @return Boolean, equality
     */
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    // Helper method that bridges gap between
    // internal java reperesentation of objects and users view of lox objects

    /**
     * Converts a lox value into a user readable string
     * @param object lox value object
     * @return string representation
     */
    private String stringify(Object object){
        if (object == null) return "nil";
        if (object instanceof Double){
            String text = object.toString();
            if (text.endsWith(".0")){ // Remove .0 from integers
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }


    /**
     * Takes in an operand and operator of a unary expression and checks whether operand is a number
     * @param operator Expression operator
     * @param operand Expression operand
     */
    private void checkNumberOperand(Token operator, Object operand){
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number!");
    }


    /**
     * Takes in operands and operator of binary expression and checks whether operands are numbers
     * @param operator Expression operator
     * @param left Expression left operand
     * @param right Expression right operand
     */
    private void checkNumberOperands(Token operator, Object left, Object right){
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be a numbers!");
    }


}
