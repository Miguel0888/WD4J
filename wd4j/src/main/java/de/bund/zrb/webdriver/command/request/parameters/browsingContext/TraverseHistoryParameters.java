package de.bund.zrb.webdriver.command.request.parameters.browsingContext;

import de.bund.zrb.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.websocket.WDCommand;

/**
 * The browsingContext.traverseHistory command traverses the history of a given navigable by a delta.
 */
public class TraverseHistoryParameters implements WDCommand.Params {
    private final WDBrowsingContext context;
    private final long delta;

    public TraverseHistoryParameters(WDBrowsingContext browsingContext, long delta) {
        this.context = browsingContext;
        this.delta = delta;
    }

    public WDBrowsingContext getBrowsingContext() {
        return context;
    }

    public long getDelta() {
        return delta;
    }
}
