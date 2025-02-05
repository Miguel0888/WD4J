package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;
import wd4j.impl.webdriver.type.script.remoteValue.support.WindowProxyProperties;

public class WindowProxyRemoteValue extends RemoteValue {
    private final WindowProxyProperties value;

    public WindowProxyRemoteValue(String handle, String internalId, WindowProxyProperties value) {
        super("window", handle, internalId);
        this.value = value;
    }

    public WindowProxyProperties getValue() {
        return value;
    }
}
