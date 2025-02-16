package wd4j.impl.webdriver.type.script;

public class WDStackFrame {
    private final String functionName;
    private final String url;
    private final int lineNumber;
    private final int columnNumber;

    public WDStackFrame(String functionName, String url, int lineNumber, int columnNumber) {
        this.functionName = functionName;
        this.url = url;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getUrl() {
        return url;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }
}