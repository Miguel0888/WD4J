package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

/**
 * The browsingContext.traverseHistory command traverses the history of a given navigable by a delta.
 */
public class TraverseHistoryParameters implements Command.Params {
    private final BrowsingContext browsingContext;
    private final long delta;

    public TraverseHistoryParameters(BrowsingContext browsingContext, long delta) {
        this.browsingContext = browsingContext;
        this.delta = delta;
    }

    public BrowsingContext getBrowsingContext() {
        return browsingContext;
    }

    public long getDelta() {
        return delta;
    }
}
