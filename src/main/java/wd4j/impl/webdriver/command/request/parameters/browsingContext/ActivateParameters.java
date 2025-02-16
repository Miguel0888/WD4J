package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.websocket.WDCommand;

public class ActivateParameters implements WDCommand.Params {
    private final WDBrowsingContext context;

    public ActivateParameters(WDBrowsingContext context) {
        this.context = context;
    }

    public WDBrowsingContext getContext() {
        return context;
    }
}
