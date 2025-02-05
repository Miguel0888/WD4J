package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;
import java.util.Map;

public class ObjectRemoteValue extends RemoteValue {
    private final Map<String, RemoteValue> value;

    public ObjectRemoteValue(String handle, String internalId, Map<String, RemoteValue> value) {
        super("object", handle, internalId);
        this.value = value;
    }

    public Map<String, RemoteValue> getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ObjectRemoteValue{" +
                "type='" + getType() + '\'' +
                ", handle='" + getHandle() + '\'' +
                ", internalId='" + getInternalId() + '\'' +
                ", value=" + value +
                '}';
    }
}
