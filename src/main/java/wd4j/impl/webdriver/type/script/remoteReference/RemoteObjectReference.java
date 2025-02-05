package wd4j.impl.webdriver.type.script.remoteReference;

import wd4j.impl.webdriver.type.script.Handle;
import wd4j.impl.webdriver.type.script.RemoteReference;

public class RemoteObjectReference extends RemoteReference {
    String sharedId;

    public RemoteObjectReference(Handle handle, String sharedId) {
        super(handle);
        if(handle == null) {
            throw new IllegalArgumentException("Handle cannot be null");
        }
        this.sharedId = sharedId;
    }

    public String getSharedId() {
        return sharedId;
    }
}
