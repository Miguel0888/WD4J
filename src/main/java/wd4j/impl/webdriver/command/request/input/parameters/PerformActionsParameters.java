package wd4j.impl.webdriver.command.request.input.parameters;

import wd4j.impl.webdriver.command.request.browsingContext.BrowsingContext;

import java.util.List;

public class PerformActionsParameters {
    private final BrowsingContext browsingContext;
    private final List<SourceActions> actions;

    public PerformActionsParameters(BrowsingContext browsingContext, List<SourceActions> actions) {
        this.browsingContext = browsingContext;
        this.actions = actions;
    }

    public BrowsingContext getBrowsingContext() {
        return browsingContext;
    }
}
