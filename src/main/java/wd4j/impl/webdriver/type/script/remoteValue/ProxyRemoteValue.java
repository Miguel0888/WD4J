package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class ProxyRemoteValue extends RemoteValue {
    public ProxyRemoteValue(String handle, String internalId) {
        super("proxy", handle, internalId);
    }
}
