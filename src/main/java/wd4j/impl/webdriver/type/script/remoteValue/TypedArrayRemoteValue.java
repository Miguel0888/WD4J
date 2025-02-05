package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class TypedArrayRemoteValue extends RemoteValue {
    public TypedArrayRemoteValue(String handle, String internalId) {
        super("typedarray", handle, internalId);
    }
}
