package de.bund.zrb.command.request.parameters.input;

import de.bund.zrb.command.request.parameters.input.sourceActions.SourceActions;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.api.WDCommand;

import java.util.List;

public class PerformActionsParameters implements WDCommand.Params {
    private final WDBrowsingContext context;
    private final List<SourceActions> actions;

    public PerformActionsParameters(WDBrowsingContext context, List<SourceActions> actions) {
        this.context = context;
        this.actions = actions;
    }

    public WDBrowsingContext getBrowsingContext() {
        return context;
    }

}
