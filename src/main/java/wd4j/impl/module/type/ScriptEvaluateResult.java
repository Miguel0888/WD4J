package wd4j.impl.module.type;

public class ScriptEvaluateResult {
    private final Object result;
    private final ScriptExceptionDetails exceptionDetails;

    public ScriptEvaluateResult(Object result, ScriptExceptionDetails exceptionDetails) {
        this.result = result;
        this.exceptionDetails = exceptionDetails;
    }

    public Object getResult() {
        return result;
    }

    public ScriptExceptionDetails getExceptionDetails() {
        return exceptionDetails;
    }
}