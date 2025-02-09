package wd4j.impl.webdriver.type.storage.parameters;

import wd4j.impl.websocket.Command;

public class DeleteCookieParameters implements Command.Params {
    private final CookieFilter filter;
    private final PartitionDescriptor partition;

    public DeleteCookieParameters(CookieFilter filter, PartitionDescriptor partition) {
        this.filter = filter;
        this.partition = partition;
    }

    public CookieFilter getFilter() {
        return filter;
    }

    public PartitionDescriptor getPartition() {
        return partition;
    }
}
