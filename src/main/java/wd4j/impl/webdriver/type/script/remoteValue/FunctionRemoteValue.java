package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class FunctionRemoteValue extends RemoteValue {

    public FunctionRemoteValue(String handle, String internalId) {
        super("function", handle, internalId);
    }

    @Override
    public String toString() {
        return "FunctionRemoteValue{" +
                "type='" + getType() + '\'' +
                ", handle='" + getHandle() + '\'' +
                ", internalId='" + getInternalId() + '\'' +
                '}';
    }
}
