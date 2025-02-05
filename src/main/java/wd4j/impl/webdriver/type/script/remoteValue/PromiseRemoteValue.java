package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class PromiseRemoteValue extends RemoteValue {
    public PromiseRemoteValue(String handle, String internalId) {
        super("promise", handle, internalId);
    }
}
