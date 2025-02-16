package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.script.WDStackTrace;

public class WDInitiator {
    private final Character colomnNumber;
    private final Character lineNumber;
    private final WDRequest WDRequest;
    private final WDStackTrace WDStackTrace;
    private final Type type;

    public WDInitiator(Type type) {
        this.type = type;
        this.colomnNumber = null;
        this.lineNumber = null;
        this.WDRequest = null;
        this.WDStackTrace = null;

    }

    public WDInitiator(Type type, Character colomnNumber, Character lineNumber, WDRequest WDRequest, WDStackTrace WDStackTrace) {
        this.type = type;
        this.colomnNumber = colomnNumber;
        this.lineNumber = lineNumber;
        this.WDRequest = WDRequest;
        this.WDStackTrace = WDStackTrace;
    }

    public Character getColomnNumber() {
        return colomnNumber;
    }

    public Character getLineNumber() {
        return lineNumber;
    }

    public WDRequest getRequest() {
        return WDRequest;
    }

    public WDStackTrace getStackTrace() {
        return WDStackTrace;
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