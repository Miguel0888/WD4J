package de.bund.zrb.runtime;

import java.util.Map;

/**
 * Read-only Sicht auf die aktuell gültigen Variablen & Templates.
 *
 * Wird pro Action neu gebaut (über runtimeCtx.buildCaseScope()).
 */
public class ValueScope {

    private final Map<String,String> rootVars;
    private final Map<String,String> suiteVars;
    private final Map<String,String> caseVars;

    private final Map<String,String> rootTemplates;
    private final Map<String,String> suiteTemplates;
    private final Map<String,String> caseTemplates;

    private final ExpressionRegistry exprRegistry;

    public ValueScope(
            Map<String,String> rootVars,
            Map<String,String> suiteVars,
            Map<String,String> caseVars,
            Map<String,String> rootTemplates,
            Map<String,String> suiteTemplates,
            Map<String,String> caseTemplates,
            ExpressionRegistry exprRegistry
    ) {
        this.rootVars        = rootVars;
        this.suiteVars       = suiteVars;
        this.caseVars        = caseVars;
        this.rootTemplates   = rootTemplates;
        this.suiteTemplates  = suiteTemplates;
        this.caseTemplates   = caseTemplates;
        this.exprRegistry    = exprRegistry;
    }

    /**
     * Shadowing Lookup für normale Variablenwerte.
     */
    public String lookupVar(String name) {
        if (name == null) return null;
        if (caseVars != null && caseVars.containsKey(name)) {
            return caseVars.get(name);
        }
        if (suiteVars != null && suiteVars.containsKey(name)) {
            return suiteVars.get(name);
        }
        if (rootVars != null && rootVars.containsKey(name)) {
            return rootVars.get(name);
        }
        return null;
    }

    /**
     * Shadowing Lookup für Templates.
     * Templates sind faule Ausdrücke wie "{{otp()}}".
     */
    public String lookupTemplate(String name) {
        if (name == null) return null;
        if (caseTemplates != null && caseTemplates.containsKey(name)) {
            return caseTemplates.get(name);
        }
        if (suiteTemplates != null && suiteTemplates.containsKey(name)) {
            return suiteTemplates.get(name);
        }
        if (rootTemplates != null && rootTemplates.containsKey(name)) {
            return rootTemplates.get(name);
        }
        return null;
    }

    /**
     * Zugriff auf deine Function-Registry (ExpressionRegistryImpl).
     * Die brauchen wir für Funktionsaufrufe otp(), wrap(), etc.
     */
    public ExpressionRegistry getExpressionRegistry() {
        return exprRegistry;
    }
}
