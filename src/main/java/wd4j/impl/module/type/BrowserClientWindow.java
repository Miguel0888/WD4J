package wd4j.impl.module.type;

public class BrowserClientWindow {
    private final String id;

    public BrowserClientWindow(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }
}