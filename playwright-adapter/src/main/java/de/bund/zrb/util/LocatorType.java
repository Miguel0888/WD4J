package de.bund.zrb.util;

public enum LocatorType {
    CSS("css"),
    XPATH("xpath"),
    ID("id"),
    TEXT("text"),
    ROLE("role"),
    LABEL("label"),
    PLACEHOLDER("placeholder"),
    ALTTEXT("altText");

    private final String key;

    LocatorType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static LocatorType fromKey(String key) {
        if (key == null) return null;
        for (LocatorType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
