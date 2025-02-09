package wd4j.impl.webdriver.command.request.browsingContext.parameters;

public enum Orientation {
    LANDSCAPE("landscape"),
    PORTRAIT("portrait");

    private final String value;

    Orientation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
