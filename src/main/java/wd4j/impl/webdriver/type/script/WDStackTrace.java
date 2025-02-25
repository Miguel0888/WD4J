package wd4j.impl.webdriver.type.script;

import java.util.List;

public class WDStackTrace {
    private final List<StackFrame> frames;

    public WDStackTrace(List<StackFrame> frames) {
        this.frames = frames;
    }

    public List<StackFrame> getFrames() {
        return frames;
    }

    public static class StackFrame {
        private final String functionName;
        private final String url;
        private final int lineNumber;
        private final int columnNumber;

        public StackFrame(String functionName, String url, int lineNumber, int columnNumber) {
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
}