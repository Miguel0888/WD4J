package de.bund.zrb.impl.webdriver.command.request.parameters.input;

import de.bund.zrb.impl.webdriver.command.request.parameters.input.sourceActions.SourceActions;
import de.bund.zrb.impl.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.impl.websocket.WDCommand;

import java.util.List;

public class PerformActionsParameters implements WDCommand.Params {
    private final WDBrowsingContext WDBrowsingContext;
    private final List<SourceActions> actions;

    public PerformActionsParameters(WDBrowsingContext WDBrowsingContext, List<SourceActions> actions) {
        this.WDBrowsingContext = WDBrowsingContext;
        this.actions = actions;
    }

    public WDBrowsingContext getBrowsingContext() {
        return WDBrowsingContext;
    }

}
