package wd4j.impl.webdriver.type.script;

public class ExceptionDetails {
    private final char coloumnNumber;
    private final RemoteValue exception;
    private final char lineNumber;
    private final StackTrace stackTrace;
    private final String text;

    public ExceptionDetails(char coloumnNumber, RemoteValue exception, char lineNumber, StackTrace stackTrace, String text) {
        this.coloumnNumber = coloumnNumber;
        this.exception = exception;
        this.lineNumber = lineNumber;
        this.stackTrace = stackTrace;
        this.text = text;
    }

    public char getColoumnNumber() {
        return coloumnNumber;
    }

    public RemoteValue getException() {
        return exception;
    }

    public char getLineNumber() {
        return lineNumber;
    }

    public StackTrace getStackTrace() {
        return stackTrace;
    }

    public String getText() {
        return text;
    }
}