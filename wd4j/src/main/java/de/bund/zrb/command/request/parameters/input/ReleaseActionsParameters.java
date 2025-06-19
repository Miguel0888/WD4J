package de.bund.zrb.command.request.parameters.input;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.api.WDCommand;

public class ReleaseActionsParameters implements WDCommand.Params {
    private final WDBrowsingContext WDBrowsingContext;

    public ReleaseActionsParameters(WDBrowsingContext WDBrowsingContext) {
        this.WDBrowsingContext = WDBrowsingContext;
    }

    public WDBrowsingContext getBrowsingContext() {
        return WDBrowsingContext;
    }
}
