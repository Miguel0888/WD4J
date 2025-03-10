package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.websocket.WDCommand;

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
