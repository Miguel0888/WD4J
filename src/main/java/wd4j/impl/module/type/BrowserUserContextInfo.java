package wd4j.impl.module.type;

public class BrowserUserContextInfo {
    private final String id;
    private final String type;

    public BrowserUserContextInfo(String id, String type) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}