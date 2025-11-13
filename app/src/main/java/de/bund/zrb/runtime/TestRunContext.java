package de.bund.zrb.runtime;

public final class TestRunContext {

    private final RuntimeVariableContext vars;

    private boolean rootBeforeAllDone = false;
    private final java.util.Set<String> suiteBeforeAllDone = new java.util.HashSet<String>();
    private final java.util.Set<String> caseBeforeChainDone = new java.util.HashSet<String>();

    public TestRunContext(ExpressionRegistry exprRegistry) {
        this.vars = new RuntimeVariableContext(exprRegistry);
    }

    public RuntimeVariableContext getVars() {
        return vars;
    }

    public boolean isRootBeforeAllDone() {
        return rootBeforeAllDone;
    }

    public void markRootBeforeAllDone() {
        this.rootBeforeAllDone = true;
    }

    public boolean isSuiteBeforeAllDone(String suiteId) {
        return suiteId != null && suiteBeforeAllDone.contains(suiteId);
    }

    public void markSuiteBeforeAllDone(String suiteId) {
        if (suiteId != null) {
            suiteBeforeAllDone.add(suiteId);
        }
    }

    public boolean isCaseBeforeChainDone(String caseId) {
        return caseId != null && caseBeforeChainDone.contains(caseId);
    }

    public void markCaseBeforeChainDone(String caseId) {
        if (caseId != null) {
            caseBeforeChainDone.add(caseId);
        }
    }
}
