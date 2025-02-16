package wd4j.impl.webdriver.type.script;

public class WDExceptionDetails {
    private final char coloumnNumber;
    private final WDRemoteValue exception;
    private final char lineNumber;
    private final WDStackTrace stackTrace;
    private final String text;

    public WDExceptionDetails(char coloumnNumber, WDRemoteValue exception, char lineNumber, WDStackTrace stackTrace, String text) {
        this.coloumnNumber = coloumnNumber;
        this.exception = exception;
        this.lineNumber = lineNumber;
        this.stackTrace = stackTrace;
        this.text = text;
    }

    public char getColoumnNumber() {
        return coloumnNumber;
    }

    public WDRemoteValue getException() {
        return exception;
    }

    public char getLineNumber() {
        return lineNumber;
    }

    public WDStackTrace getStackTrace() {
        return stackTrace;
    }

    public String getText() {
        return text;
    }
}