package wd4j.impl.webdriver.command.request.parameters.storage;

import wd4j.impl.websocket.Command;

public class GetCookiesParameters implements Command.Params {
    CookieFilter filter;
    SetCookieParameters.PartitionDescriptor partition;

    public GetCookiesParameters(CookieFilter filter, SetCookieParameters.PartitionDescriptor partition) {
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
