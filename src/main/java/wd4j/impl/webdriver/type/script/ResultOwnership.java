package wd4j.impl.webdriver.type.script;

public enum ResultOwnership {
    ROOT("root"),
    NONE("none");

    private final String value;

    ResultOwnership(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
