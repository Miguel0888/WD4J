package wd4j.impl.dto.command.request.parameters.storage;

import wd4j.impl.websocket.WDCommand;

public class DeleteCookiesParameters implements WDCommand.Params {
    private final CookieFilter filter; // Optional
    private final PartitionDescriptor partition; // Optional

    public DeleteCookiesParameters() {
        this(null, null);
    }

    public DeleteCookiesParameters(CookieFilter filter) {
        this(filter, null);
    }

    public DeleteCookiesParameters(PartitionDescriptor partition) {
        this(null, partition);
    }

    public DeleteCookiesParameters(CookieFilter filter, PartitionDescriptor partition) {
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
