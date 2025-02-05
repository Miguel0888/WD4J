package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class ErrorRemoteValue extends RemoteValue {
    public ErrorRemoteValue(String handle, String internalId) {
        super("error", handle, internalId);
    }
}
