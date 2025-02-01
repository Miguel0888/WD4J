package wd4j.impl.module.type;

public class NetworkCookie {
    private final String name;
    private final String value;

    public NetworkCookie(String name, String value) {
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