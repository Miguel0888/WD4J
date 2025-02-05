package wd4j.impl.webdriver.type.input.sourceActions;

public enum PointerType {
    MOUSE("mouse"),
    PEN("pen"),
    TOUCH("touch");

    private final String value;

    PointerType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
