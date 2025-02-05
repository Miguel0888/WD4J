package wd4j.impl.webdriver.type.script.remoteValue;

import java.util.List;
import wd4j.impl.webdriver.type.script.RemoteValue;

public class NodeListRemoteValue {
    private final List<RemoteValue> values;

    public NodeListRemoteValue(List<RemoteValue> values) {
        this.values = values;
    }

    public List<RemoteValue> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return "ListRemoteValue{" +
                "values=" + values +
                '}';
    }
}
