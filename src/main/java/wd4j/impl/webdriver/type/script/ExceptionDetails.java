package wd4j.impl.webdriver.type.script;

public class ExceptionDetails {
    private final String text;
    private final StackTrace stackTrace;

    public ExceptionDetails(String text, StackTrace stackTrace) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text must not be null or empty.");
        }
        this.text = text;
        this.stackTrace = stackTrace;
    }

    public String getText() {
        return text;
    }

    public StackTrace getStackTrace() {
        return stackTrace;
    }
}