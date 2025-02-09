package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.ReadinessState;
import wd4j.impl.websocket.Command;

public class ReloadParameters implements Command.Params {
    private final BrowsingContext context;
    private final Boolean ignoreCache; // optional
    private final ReadinessState wait; // optional

    public ReloadParameters(BrowsingContext context) {
        this(context, null, null);
    }

    public ReloadParameters(BrowsingContext context, Boolean ignoreCache, ReadinessState wait) {
        this.context = context;
        this.ignoreCache = ignoreCache;
        this.wait = wait;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public Boolean getIgnoreCache() {
        return ignoreCache;
    }

    public ReadinessState getWait() {
        return wait;
    }
}
