package wd4j.impl.webdriver.type.network.parameters;

public enum CacheBehavior {
    DEFAULT("default"),
    BYPASS("bypass");

    private final String value;

    private CacheBehavior(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
