package de.bund.zrb.command.request.parameters.browsingContext;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.api.WDCommand;

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
