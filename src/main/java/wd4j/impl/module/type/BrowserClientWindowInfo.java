package wd4j.impl.module.type;

public class BrowserClientWindowInfo {
    private final String id;
    private final String state;

    public BrowserClientWindowInfo(String id, String state) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        if (state == null || state.isEmpty()) {
            throw new IllegalArgumentException("State must not be null or empty.");
        }
        this.id = id;
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public String getState() {
        return state;
    }
}