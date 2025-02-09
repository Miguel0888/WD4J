package wd4j.impl.webdriver.command.request.input.parameters;

import wd4j.impl.webdriver.command.request.browsingContext.BrowsingContext;

public class ReleaseActionsParameters {
    private final BrowsingContext browsingContext;

    public ReleaseActionsParameters(BrowsingContext browsingContext) {
        this.browsingContext = browsingContext;
    }

    public BrowsingContext getBrowsingContext() {
        return browsingContext;
    }
}
