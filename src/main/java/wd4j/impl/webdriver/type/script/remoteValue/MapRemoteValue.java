package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class MapRemoteValue extends RemoteValue {
    private final MappingRemoteValue value;

    public MapRemoteValue(String handle, String internalId, MappingRemoteValue value) {
        super("map", handle, internalId);
        this.value = value;
    }

    public MappingRemoteValue getValue() {
        return value;
    }
}
