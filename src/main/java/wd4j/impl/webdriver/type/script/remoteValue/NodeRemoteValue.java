package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;
import wd4j.impl.webdriver.type.script.remoteValue.support.NodeProperties;

public class NodeRemoteValue extends RemoteValue {
    private final String sharedId;
    private final NodeProperties value;

    public NodeRemoteValue(String sharedId, String handle, String internalId, NodeProperties value) {
        super("node", handle, internalId);
        this.sharedId = sharedId;
        this.value = value;
    }

    public String getSharedId() {
        return sharedId;
    }

    public NodeProperties getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "NodeRemoteValue{" +
                "type='" + getType() + '\'' +
                ", sharedId='" + sharedId + '\'' +
                ", handle='" + getHandle() + '\'' +
                ", internalId='" + getInternalId() + '\'' +
                ", value=" + value +
                '}';
    }
}
