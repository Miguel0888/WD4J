package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class SymbolRemoteValue extends RemoteValue {

    public SymbolRemoteValue(String handle, String internalId) {
        super("symbol", handle, internalId);
    }

    @Override
    public String toString() {
        return "SymbolRemoteValue{" +
                "type='" + getType() + '\'' +
                ", handle='" + getHandle() + '\'' +
                ", internalId='" + getInternalId() + '\'' +
                '}';
    }
}
