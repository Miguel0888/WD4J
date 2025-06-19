package de.bund.zrb.webdriver.command.request.parameters.browsingContext;

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
