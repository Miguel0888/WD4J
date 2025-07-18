package de.bund.zrb.command.request.parameters.storage;

import de.bund.zrb.api.WDCommand;

public class GetCookiesParameters implements WDCommand.Params {
    private final CookieFilter filter; // Optional
    private final PartitionDescriptor partition; // Optional

    public GetCookiesParameters() {
        this(null, null);
    }

    public GetCookiesParameters(CookieFilter filter) {
        this(filter, null);
    }

    public GetCookiesParameters(PartitionDescriptor partition) {
        this(null, partition);
    }

    public GetCookiesParameters(CookieFilter filter, PartitionDescriptor partition) {
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
