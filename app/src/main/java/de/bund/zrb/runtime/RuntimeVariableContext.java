package de.bund.zrb.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hold the hierarchical runtime variables during playback.
 *
 * Intent:
 *  - Keep variables for root, for current suite, and for current test case.
 *  - Allow setting variables at each level.
 *  - Expose ValueScopes for evaluation.
 *
 * Design:
 *  rootScope <- parent of suiteScope <- parent of caseScope
 *
 * Usage:
 *  RuntimeVariableContext ctx = new RuntimeVariableContext(ExpressionRegistryImpl.getInstance());
 *  ctx.setRootVar("env", "INT");
 *  ctx.enterSuite();
 *  ctx.setSuiteVar("mandant", "4711");
 *  ctx.enterCase();
 *  ctx.setCaseVar("username", "alice");
 *  ValueScope scope = ctx.buildCaseScope();
 *  // eval with scope...
 */
public class RuntimeVariableContext {

    private final ExpressionRegistry registry;

    private final Map<String, String> rootVars  = new LinkedHashMap<String, String>();
    private final Map<String, String> suiteVars = new LinkedHashMap<String, String>();
    private final Map<String, String> caseVars  = new LinkedHashMap<String, String>();

    public RuntimeVariableContext(ExpressionRegistry registry) {
        this.registry = registry;
    }

    // ---- lifecycle ----------------------------------------------------------

    /**
     * Reset suiteVars and caseVars.
     * Call when starting a new suite.
     */
    public void enterSuite() {
        suiteVars.clear();
        caseVars.clear();
    }

    /**
     * Reset caseVars only.
     * Call when starting a new test case.
     */
    public void enterCase() {
        caseVars.clear();
    }

    // ---- setters ------------------------------------------------------------

    /** Set a root/global variable (lives across whole run). */
    public void setRootVar(String name, String value) {
        if (name != null && value != null) {
            rootVars.put(name, value);
        }
    }

    /** Set a suite-level variable (for current suite). */
    public void setSuiteVar(String name, String value) {
        if (name != null && value != null) {
            suiteVars.put(name, value);
        }
    }

    /** Set a case-level variable (for current test case). */
    public void setCaseVar(String name, String value) {
        if (name != null && value != null) {
            caseVars.put(name, value);
        }
    }

    // ---- scope building -----------------------------------------------------

    /**
     * Build a ValueScope chain: case -> suite -> root.
     * This is what the ActionRuntimeEvaluator should use.
     */
    public ValueScope buildCaseScope() {
        // root
        ValueScope rootScope = new ValueScope(registry, null);
        putAllIntoScope(rootScope, rootVars);

        // suite
        ValueScope suiteScope = new ValueScope(registry, rootScope);
        putAllIntoScope(suiteScope, suiteVars);

        // case
        ValueScope caseScope = new ValueScope(registry, suiteScope);
        putAllIntoScope(caseScope, caseVars);

        return caseScope;
    }

    private void putAllIntoScope(ValueScope scope, Map<String,String> vars) {
        for (Map.Entry<String,String> e : vars.entrySet()) {
            scope.putVariable(e.getKey(), e.getValue());
        }
    }
}
