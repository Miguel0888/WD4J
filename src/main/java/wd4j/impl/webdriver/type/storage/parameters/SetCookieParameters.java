package wd4j.impl.webdriver.type.storage.parameters;

import wd4j.impl.webdriver.type.storage.PartialCookie;
import wd4j.impl.webdriver.type.storage.PartitionDescriptor;
import wd4j.impl.websocket.Command;

public class SetCookieParameters implements Command.Params {
    PartialCookie cookie;
    PartitionDescriptor partition;

    public SetCookieParameters(PartialCookie cookie, PartitionDescriptor partition) {
        this.cookie = cookie;
        this.partition = partition;
    }

    public PartialCookie getCookie() {
        return cookie;
    }

    public PartitionDescriptor getPartition() {
        return partition;
    }
}
