package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.script.StackTrace;

public class Initiator {
    private final Character colomnNumber;
    private final Character lineNumber;
    private final Request request;
    private final StackTrace stackTrace;
    private final Type type;

    public Initiator(Type type) {
        this.type = type;
        this.colomnNumber = null;
        this.lineNumber = null;
        this.request = null;
        this.stackTrace = null;

    }

    public Initiator(Type type, Character colomnNumber, Character lineNumber, Request request, StackTrace stackTrace) {
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

    public Request getRequest() {
        return request;
    }

    public StackTrace getStackTrace() {
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