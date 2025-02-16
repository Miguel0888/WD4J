package wd4j.impl.webdriver.type.error;

import wd4j.impl.webdriver.mapping.EnumWrapper;

public enum WDErrorCode implements EnumWrapper {
    INVALID_ARGUMENT("invalid argument"),
    INVALID_SELECTOR("invalid selector"),
    INVALID_SESSION_ID("invalid session id"),
    INVALID_WEB_EXTENSION("invalid web extension"),
    MOVE_TARGET_OUT_OF_BOUNDS("move target out of bounds"),
    NO_SUCH_ALERT("no such alert"),
    NO_SUCH_ELEMENT("no such element"),
    NO_SUCH_FRAME("no such frame"),
    NO_SUCH_HANDLE("no such handle"),
    NO_SUCH_HISTORY_ENTRY("no such history entry"),
    NO_SUCH_INTERCEPT("no such intercept"),
    NO_SUCH_NODE("no such node"),
    NO_SUCH_REQUEST("no such request"),
    NO_SUCH_SCRIPT("no such script"),
    NO_SUCH_STORAGE_PARTITION("no such storage partition"),
    NO_SUCH_USER_CONTEXT("no such user context"),
    NO_SUCH_WEB_EXTENSION("no such web extension"),
    SESSION_NOT_CREATED("session not created"),
    UNABLE_TO_CAPTURE_SCREEN("unable to capture screen"),
    UNABLE_TO_CLOSE_BROWSER("unable to close browser"),
    UNABLE_TO_SET_COOKIE("unable to set cookie"),
    UNABLE_TO_SET_FILE_INPUT("unable to set file input"),
    UNDERSPECIFIED_STORAGE_PARTITION("underspecified storage partition"),
    UNKNOWN_COMMAND("unknown command"),
    UNKNOWN_ERROR("unknown error"),
    UNSUPPORTED_OPERATION("unsupported operation");

    private final String value;

    WDErrorCode(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }

    public static WDErrorCode fromValue(String value) {
        for (WDErrorCode code : values()) {
            if (code.value().equalsIgnoreCase(value)) {
                return code;
            }
        }
        throw new IllegalArgumentException("Unknown ErrorCode: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
