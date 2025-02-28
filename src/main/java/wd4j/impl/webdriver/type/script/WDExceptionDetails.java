package wd4j.impl.webdriver.type.script;

public class WDExceptionDetails {
    private final long coloumnNumber;
    private final WDRemoteValue exception;
    private final long lineNumber;
    private final WDStackTrace stackTrace;
    private final String text;

    public WDExceptionDetails(long coloumnNumber, WDRemoteValue exception, long lineNumber, WDStackTrace stackTrace, String text) {
        this.coloumnNumber = coloumnNumber;
        this.exception = exception;
        this.lineNumber = lineNumber;
        this.stackTrace = stackTrace;
        this.text = text;
    }

    public long getColoumnNumber() {
        return coloumnNumber;
    }

    public WDRemoteValue getException() {
        return exception;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public WDStackTrace getStackTrace() {
        return stackTrace;
    }

    public String getText() {
        return text;
    }
}