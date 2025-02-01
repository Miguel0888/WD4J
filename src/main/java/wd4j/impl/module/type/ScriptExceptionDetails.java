package wd4j.impl.module.type;

import wd4j.impl.module.ScriptService;

public class ScriptExceptionDetails {
    private final String text;
    private final ScriptStackTrace stackTrace;

    public ScriptExceptionDetails(String text, ScriptStackTrace stackTrace) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text must not be null or empty.");
        }
        this.text = text;
        this.stackTrace = stackTrace;
    }

    public String getText() {
        return text;
    }

    public ScriptStackTrace getStackTrace() {
        return stackTrace;
    }
}