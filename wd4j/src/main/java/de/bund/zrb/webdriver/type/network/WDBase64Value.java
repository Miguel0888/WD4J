package de.bund.zrb.webdriver.type.network;

public class WDBase64Value {
    private final String type = "base64";
    private final String value;

    public WDBase64Value(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
