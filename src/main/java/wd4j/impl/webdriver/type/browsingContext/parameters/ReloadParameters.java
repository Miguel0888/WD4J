package wd4j.impl.webdriver.type.browsingContext.parameters;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.ReadinessState;
import wd4j.impl.websocket.Command;

public class ReloadParameters implements Command.Params {
    private final BrowsingContext context;
    private final boolean ignoreCache;
    private final ReadinessState wait;

    public ReloadParameters(BrowsingContext context, boolean ignoreCache, ReadinessState wait) {
        this.context = context;
        this.ignoreCache = ignoreCache;
        this.wait = wait;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public boolean getIgnoreCache() {
        return ignoreCache;
    }

    public ReadinessState getWait() {
        return wait;
    }
}
