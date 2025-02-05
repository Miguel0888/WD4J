package wd4j.impl.webdriver.type.script.remoteValue;

import java.util.List;
import wd4j.impl.webdriver.type.script.RemoteValue;

public class HTMLCollectionRemoteValue extends RemoteValue {
    private final List<RemoteValue> value;

    public HTMLCollectionRemoteValue(String handle, String internalId, List<RemoteValue> value) {
        super("htmlcollection", handle, internalId);
        this.value = value;
    }

    public List<RemoteValue> getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "HTMLCollectionRemoteValue{" +
                "type='" + getType() + '\'' +
                ", handle='" + getHandle() + '\'' +
                ", internalId='" + getInternalId() + '\'' +
                ", value=" + value +
                '}';
    }
}
