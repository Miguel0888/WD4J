package de.bund.zrb.type.network;

import de.bund.zrb.support.mapping.EnumWrapper;
import de.bund.zrb.type.script.WDStackTrace;

public class WDInitiator {
    private final Long columnNumber;
    private final Long lineNumber;
    private final WDRequest request;
    private final WDStackTrace stackTrace;
    private final Type type;

    public WDInitiator(Type type) {
        this.type = type;
        this.columnNumber = null;
        this.lineNumber = null;
        this.request = null;
        this.stackTrace = null;

    }

    public WDInitiator(Type type, Long columnNumber, Long lineNumber, WDRequest request, WDStackTrace stackTrace) {
        this.type = type;
        this.columnNumber = columnNumber;
        this.lineNumber = lineNumber;
        this.request = request;
        this.stackTrace = stackTrace;
    }

    public Long getColumnNumber() {
        return columnNumber;
    }

    public Long getLineNumber() {
        return lineNumber;
    }

    public WDRequest getRequest() {
        return request;
    }

    public WDStackTrace getStackTrace() {
        return stackTrace;
    }

    public Type getType() {
        return type;
    }

    public enum Type implements EnumWrapper {
        PARSER("parser"),
        SCRIPT("script"),
        PREFLIGHT("preflight"),
        OTHER("other");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        @Override // confirmed
        public String value() {
            return value;
        }
    }
}