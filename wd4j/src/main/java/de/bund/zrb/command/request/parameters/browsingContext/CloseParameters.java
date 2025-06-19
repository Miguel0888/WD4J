package de.bund.zrb.command.request.parameters.browsingContext;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.api.WDCommand;

public class CloseParameters implements WDCommand.Params {
    private final WDBrowsingContext context;
    private final Boolean promptUnload; // Optional

    public CloseParameters(WDBrowsingContext context, Boolean promptUnload) {
        this.context = context;
        this.promptUnload = promptUnload;
    }

    public CloseParameters(WDBrowsingContext context) {
        this.context = context;
        this.promptUnload = null;
    }

    public WDBrowsingContext getContext() {
        return context;
    }

    public Boolean getPromptUnload() {
        return promptUnload;
    }

}
