package wd4j.impl.webdriver.type.network;

public class StringValue implements BytesValue {
    private final String type = "string";
    private final String value;

    public StringValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
