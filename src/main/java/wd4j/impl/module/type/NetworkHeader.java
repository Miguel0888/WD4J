package wd4j.impl.module.type;

public class NetworkHeader {
    private final String name;
    private final String value;

    public NetworkHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}