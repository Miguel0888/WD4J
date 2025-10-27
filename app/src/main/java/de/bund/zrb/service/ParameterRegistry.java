package de.bund.zrb.service;

import java.util.*;

/**
 * Keep and resolve test parameters for the current run.
 * Support nested scopes (Suite -> Case). The top scope shadows lower scopes.
 */
public class ParameterRegistry {

    private static final ParameterRegistry INSTANCE = new ParameterRegistry();

    // Each scope is a map: parameterName -> currentValue (null allowed)
    private final Deque<Map<String, String>> scopeStack = new ArrayDeque<Map<String, String>>();

    private ParameterRegistry() {
        // Hide constructor
    }

    public static ParameterRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Open a new scope (e.g. on entering a Suite or a TestCase).
     */
    public synchronized void pushScope(String scopeName) {
        scopeStack.push(new HashMap<String, String>());
    }

    /**
     * Close the current scope.
     */
    public synchronized void popScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        }
    }

    /**
     * Declare a parameter in the current (top) scope.
     * Example: declareParameter("Belegnummer");
     * Initial value is null until it is explicitly filled.
     */
    public synchronized void declareParameter(String name) {
        ensureScope();
        Map<String, String> top = scopeStack.peek();
        top.put(name, null);
    }

    /**
     * Assign a value to a parameter in the top scope (shadow lower scopes if needed).
     */
    public synchronized void setValue(String name, String value) {
        ensureScope();
        Map<String, String> top = scopeStack.peek();
        top.put(name, value);
    }

    /**
     * Resolve a parameter value by searching scopes top-down.
     * Return null if not found or not assigned yet.
     */
    public synchronized String getValue(String name) {
        for (Map<String, String> scope : scopeStack) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    /**
     * Return all known parameter names in the current stack.
     * Use this to populate dropdowns.
     */
    public synchronized List<String> getAllParameterNames() {
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        for (Map<String, String> scope : scopeStack) {
            names.addAll(scope.keySet());
        }
        return new ArrayList<String>(names);
    }

    private void ensureScope() {
        if (scopeStack.isEmpty()) {
            scopeStack.push(new HashMap<String, String>());
        }
    }
}
