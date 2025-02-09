package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.command.request.BrowsingContextRequest;
import wd4j.impl.websocket.Command;

public class ActivateParameters implements Command.Params {
    private final BrowsingContextRequest context;

    public ActivateParameters(BrowsingContextRequest context) {
        this.context = context;
    }

    public BrowsingContextRequest getContext() {
        return context;
    }
}
