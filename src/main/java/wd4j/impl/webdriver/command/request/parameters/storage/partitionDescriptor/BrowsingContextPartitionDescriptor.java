package wd4j.impl.webdriver.command.request.parameters.storage.partitionDescriptor;

import wd4j.impl.webdriver.command.request.BrowsingContextRequest;
import wd4j.impl.webdriver.command.request.parameters.storage.SetCookieParameters;

public class BrowsingContextPartitionDescriptor extends SetCookieParameters.PartitionDescriptor {
    private final BrowsingContextRequest context;

    public BrowsingContextPartitionDescriptor(BrowsingContextRequest context) {
        super("context");
        this.context = context;
    }

    public BrowsingContextRequest getContext() {
        return context;
    }
}
