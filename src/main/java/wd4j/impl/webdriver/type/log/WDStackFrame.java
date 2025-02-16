package wd4j.impl.webdriver.type.log;

public class WDStackFrame {
    private final char columnNumber;
    private final String functionName;
    private final char lineNumber;
    private final String url;

    public WDStackFrame(char columnNumber, String functionName, char lineNumber, String url) {
        this.columnNumber = columnNumber;
        this.functionName = functionName;
        this.lineNumber = lineNumber;
        this.url = url;
    }

    public char getColumnNumber() {
        return columnNumber;
    }

    public String getFunctionName() {
        return functionName;
    }

    public char getLineNumber() {
        return lineNumber;
    }

    public String getUrl() {
        return url;
    }
}
