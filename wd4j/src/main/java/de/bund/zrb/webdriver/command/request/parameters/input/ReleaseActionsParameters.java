package de.bund.zrb.webdriver.command.request.parameters.input;

import de.bund.zrb.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.websocket.WDCommand;

public class ReleaseActionsParameters implements WDCommand.Params {
    private final WDBrowsingContext WDBrowsingContext;

    public ReleaseActionsParameters(WDBrowsingContext WDBrowsingContext) {
        this.WDBrowsingContext = WDBrowsingContext;
    }

    public WDBrowsingContext getBrowsingContext() {
        return WDBrowsingContext;
    }
}
