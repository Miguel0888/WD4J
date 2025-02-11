package wd4j.impl.webdriver.command.request.parameters.input;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class ReleaseActionsParameters implements Command.Params {
    private final BrowsingContext browsingContext;

    public ReleaseActionsParameters(BrowsingContext browsingContext) {
        this.browsingContext = browsingContext;
    }

    public BrowsingContext getBrowsingContext() {
        return browsingContext;
    }
}
