package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class PrimitiveProtocolValue extends RemoteValue {
    private final Object value;

    public PrimitiveProtocolValue(String type, Object value) {
        super(type, null, null); // `handle` und `internalId` sind null
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "PrimitiveRemoteValue{" +
                "type='" + getType() + '\'' +
                ", value=" + value +
                '}';
    }
}
