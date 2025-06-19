package de.bund.zrb.webdriver.command.request.parameters.browsingContext;

import de.bund.zrb.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.websocket.WDCommand;

public class ActivateParameters implements WDCommand.Params {
    private final WDBrowsingContext context;

    public ActivateParameters(WDBrowsingContext context) {
        this.context = context;
    }

    public WDBrowsingContext getContext() {
        return context;
    }
}
