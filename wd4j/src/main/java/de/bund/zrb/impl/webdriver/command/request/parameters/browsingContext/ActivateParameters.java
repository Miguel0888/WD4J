package de.bund.zrb.impl.webdriver.command.request.parameters.browsingContext;

import de.bund.zrb.impl.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.impl.websocket.WDCommand;

public class ActivateParameters implements WDCommand.Params {
    private final WDBrowsingContext context;

    public ActivateParameters(WDBrowsingContext context) {
        this.context = context;
    }

    public WDBrowsingContext getContext() {
        return context;
    }
}
