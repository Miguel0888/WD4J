package wd4j.impl.webdriver.type.script.remoteReference;

import wd4j.impl.webdriver.type.script.Handle;
import wd4j.impl.webdriver.type.script.RemoteReference;

public class SharedReference extends RemoteReference {
    String sharedId;
    Handle handle;

    public SharedReference(String sharedId, Handle handle) {
        super(handle);
        if(sharedId == null) {
            throw new IllegalArgumentException("sharedId cannot be null");
        }
        this.sharedId = sharedId;
    }

    public String getSharedId() {
        return sharedId;
    }
}
