package wd4j.impl.webdriver.type.script.remoteValue;

import java.util.Map;
import wd4j.impl.webdriver.type.script.RemoteValue;

public class MappingRemoteValue {
    private final Map<Object, RemoteValue> mapping;

    public MappingRemoteValue(Map<Object, RemoteValue> mapping) {
        this.mapping = mapping;
    }

    public Map<Object, RemoteValue> getMapping() {
        return mapping;
    }

    @Override
    public String toString() {
        return "MappingRemoteValue{" +
                "mapping=" + mapping +
                '}';
    }
}
