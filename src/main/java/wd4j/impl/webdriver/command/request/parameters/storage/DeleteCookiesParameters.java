package wd4j.impl.webdriver.command.request.parameters.storage;

import wd4j.impl.websocket.Command;

public class DeleteCookiesParameters implements Command.Params {
    private final CookieFilter filter;
    private final SetCookieParameters.PartitionDescriptor partition;

    public DeleteCookiesParameters(CookieFilter filter, SetCookieParameters.PartitionDescriptor partition) {
        this.filter = filter;
        this.partition = partition;
    }

    public CookieFilter getFilter() {
        return filter;
    }

    public SetCookieParameters.PartitionDescriptor getPartition() {
        return partition;
    }
}
