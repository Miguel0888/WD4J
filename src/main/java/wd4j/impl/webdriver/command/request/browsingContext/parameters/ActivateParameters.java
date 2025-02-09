package wd4j.impl.webdriver.command.request.browsingContext.parameters;

import wd4j.impl.webdriver.command.request.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class ActivateParameters implements Command.Params {
    private final BrowsingContext context;

    public ActivateParameters(BrowsingContext context) {
        this.context = context;
    }

    public BrowsingContext getContext() {
        return context;
    }
}
