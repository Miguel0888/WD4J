package wd4j.impl.webdriver.command.request.storage.parameters.partitionDescriptor;

import wd4j.impl.webdriver.command.request.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.command.request.storage.parameters.PartitionDescriptor;

public class BrowsingContextPartitionDescriptor extends PartitionDescriptor {
    private final BrowsingContext context;

    public BrowsingContextPartitionDescriptor(BrowsingContext context) {
        super("context");
        this.context = context;
    }

    public BrowsingContext getContext() {
        return context;
    }
}
