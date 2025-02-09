package wd4j.impl.webdriver.command.request.parameters.storage.partitionDescriptor;

import wd4j.impl.webdriver.command.request.BrowsingContextRequest;
import wd4j.impl.webdriver.command.request.parameters.storage.PartitionDescriptor;

public class BrowsingContextPartitionDescriptor extends PartitionDescriptor {
    private final BrowsingContextRequest context;

    public BrowsingContextPartitionDescriptor(BrowsingContextRequest context) {
        super("context");
        this.context = context;
    }

    public BrowsingContextRequest getContext() {
        return context;
    }
}
