package wd4j.impl.webdriver.command.request.parameters.input;

import wd4j.impl.webdriver.command.request.BrowsingContextRequest;

public class ReleaseActionsParameters {
    private final BrowsingContextRequest browsingContextRequest;

    public ReleaseActionsParameters(BrowsingContextRequest browsingContextRequest) {
        this.browsingContextRequest = browsingContextRequest;
    }

    public BrowsingContextRequest getBrowsingContext() {
        return browsingContextRequest;
    }
}
