package wd4j.impl.webdriver.command.request.storage.parameters;

import wd4j.impl.websocket.Command;

public class GetCookieParameters implements Command.Params {
    CookieFilter filter;
    PartitionDescriptor partition;

    public GetCookieParameters(CookieFilter filter, PartitionDescriptor partition) {
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
