package wd4j.impl.webdriver.type.script;

public class Handle {
    private final String handle;

    public Handle(String handle) {
        if (handle == null || handle.isEmpty()) {
            throw new IllegalArgumentException("Handle must not be null or empty.");
        }
        this.handle = handle;
    }

    public String getHandle() {
        return handle;
    }
}