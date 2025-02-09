package wd4j.impl.webdriver.command.request.parameters.input;

import wd4j.impl.webdriver.command.request.BrowsingContextRequest;

import java.util.List;

public class PerformActionsParameters {
    private final BrowsingContextRequest browsingContextRequest;
    private final List<SourceActions> actions;

    public PerformActionsParameters(BrowsingContextRequest browsingContextRequest, List<SourceActions> actions) {
        this.browsingContextRequest = browsingContextRequest;
        this.actions = actions;
    }

    public BrowsingContextRequest getBrowsingContext() {
        return browsingContextRequest;
    }
}
