package wd4j.impl.webdriver.type.script;

public class RemoteReference {
    private final Handle handle;

    public RemoteReference(Handle handle) {
        this.handle = handle;
    }

    public Handle getHandle() {
        return handle;
    }
}