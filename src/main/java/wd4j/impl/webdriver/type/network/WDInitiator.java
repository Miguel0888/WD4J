package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.script.WDStackTrace;

public class WDInitiator {
    private final Character colomnNumber;
    private final Character lineNumber;
    private final WDRequest request;
    private final WDStackTrace stackTrace;
    private final Type type;

    public WDInitiator(Type type) {
        this.type = type;
        this.colomnNumber = null;
        this.lineNumber = null;
        this.request = null;
        this.stackTrace = null;

    }

    public WDInitiator(Type type, Character colomnNumber, Character lineNumber, WDRequest request, WDStackTrace stackTrace) {
        this.type = type;
        this.colomnNumber = colomnNumber;
        this.lineNumber = lineNumber;
        this.request = request;
        this.stackTrace = stackTrace;
    }

    public Character getColomnNumber() {
        return colomnNumber;
    }

    public Character getLineNumber() {
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