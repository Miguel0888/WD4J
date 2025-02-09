package wd4j.impl.webdriver.command.request.parameters.network;

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
