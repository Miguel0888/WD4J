package wd4j.impl.webdriver.type.storage.partitionDescriptor;

import wd4j.impl.webdriver.command.request.BrowsingContext;
import wd4j.impl.webdriver.type.storage.PartitionDescriptor;

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
