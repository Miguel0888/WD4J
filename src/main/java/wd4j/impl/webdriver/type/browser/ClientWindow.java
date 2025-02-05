package wd4j.impl.webdriver.type.browser;

public class ClientWindow {
    private final String id;

    public ClientWindow(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }
}