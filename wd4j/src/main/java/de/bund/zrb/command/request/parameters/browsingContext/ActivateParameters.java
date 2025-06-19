package de.bund.zrb.command.request.parameters.browsingContext;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.api.WDCommand;

public class ActivateParameters implements WDCommand.Params {
    private final WDBrowsingContext context;

    public ActivateParameters(WDBrowsingContext context) {
        this.context = context;
    }

    public WDBrowsingContext getContext() {
        return context;
    }
}
