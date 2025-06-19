package de.bund.zrb.impl.webdriver.command.request.parameters.input;

import de.bund.zrb.impl.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.impl.websocket.WDCommand;

public class ReleaseActionsParameters implements WDCommand.Params {
    private final WDBrowsingContext WDBrowsingContext;

    public ReleaseActionsParameters(WDBrowsingContext WDBrowsingContext) {
        this.WDBrowsingContext = WDBrowsingContext;
    }

    public WDBrowsingContext getBrowsingContext() {
        return WDBrowsingContext;
    }
}
