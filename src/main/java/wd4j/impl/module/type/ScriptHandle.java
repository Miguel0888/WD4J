package wd4j.impl.module.type;

public class ScriptHandle {
    private final String handle;

    public ScriptHandle(String handle) {
        if (handle == null || handle.isEmpty()) {
            throw new IllegalArgumentException("Handle must not be null or empty.");
        }
        this.handle = handle;
    }

    public String getHandle() {
        return handle;
    }
}