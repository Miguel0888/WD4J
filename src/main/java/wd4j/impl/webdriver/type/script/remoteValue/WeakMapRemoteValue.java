package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class WeakMapRemoteValue extends RemoteValue {
    public WeakMapRemoteValue(String handle, String internalId) {
        super("weakmap", handle, internalId);
    }
}
