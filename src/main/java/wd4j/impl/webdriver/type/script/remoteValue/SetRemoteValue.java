package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

import java.util.List;

public class SetRemoteValue extends RemoteValue {
    private final List<RemoteValue> value;

    public SetRemoteValue(String handle, String internalId, List<RemoteValue> value) {
        super("set", handle, internalId);
        this.value = value;
    }

    public List<RemoteValue>  getValue() {
        return value;
    }
}
