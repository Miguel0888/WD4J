package wd4j.impl.webdriver.type.browser;

// ToDo: How to implement this class correctly?
public class UserContext {
    private final String id;

    public UserContext(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }
}