package wd4j.impl.webdriver.type.script;

public class InternalId {
    private final String id;

    public InternalId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }
}