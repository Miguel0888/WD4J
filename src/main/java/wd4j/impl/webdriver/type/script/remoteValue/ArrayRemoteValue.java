package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;
import java.util.List;

public class ArrayRemoteValue extends RemoteValue {
    private final List<RemoteValue> value;

    public ArrayRemoteValue(String handle, String internalId, List<RemoteValue> value) {
        super("array", handle, internalId);
        this.value = value;
    }

    public List<RemoteValue> getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ArrayRemoteValue{" +
                "type='" + getType() + '\'' +
                ", handle='" + getHandle() + '\'' +
                ", internalId='" + getInternalId() + '\'' +
                ", value=" + value +
                '}';
    }
}
