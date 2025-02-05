package wd4j.impl.webdriver.type.network;

public enum SameSite {
    STRICT("strict"),
    LAX("lax"),
    NONE("none");

    private final String value;

    private SameSite(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
