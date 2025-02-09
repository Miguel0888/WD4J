package wd4j.impl.webdriver.type.browsingContext;

public enum ReadinessState {
    NONE("none"),
    INTERACTIVE("interactive"),
    COMPLETE("complete");

    private final String value;

    ReadinessState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}