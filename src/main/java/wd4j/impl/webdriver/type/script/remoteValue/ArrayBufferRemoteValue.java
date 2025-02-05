package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class ArrayBufferRemoteValue extends RemoteValue {
    public ArrayBufferRemoteValue(String handle, String internalId) {
        super("arraybuffer", handle, internalId);
    }
}
