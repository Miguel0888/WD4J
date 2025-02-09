package wd4j.impl.webdriver.type.network;

public class Base64Value {
    private final String type = "base64";
    private final String value;

    public Base64Value(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
