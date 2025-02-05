package wd4j.impl.webdriver.type.browsingContext.parameters;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class TraverseHistoryParameters implements Command.Params {
    private final BrowsingContext browsingContext;
    private final int delta;

    public TraverseHistoryParameters(BrowsingContext browsingContext, int delta) {
        this.browsingContext = browsingContext;
        this.delta = delta;
    }

    public BrowsingContext getBrowsingContext() {
        return browsingContext;
    }

    public int getDelta() {
        return delta;
    }
}
