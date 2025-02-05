package wd4j.impl.webdriver.type.network.parameters;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

import java.util.List;

public class SetCacheBehaviorParameters implements Command.Params {
    private final String cacheBehavior;
    private final List<BrowsingContext> contexts;

    public SetCacheBehaviorParameters(String cacheBehavior, List<BrowsingContext> contexts) {
        this.cacheBehavior = cacheBehavior;
        this.contexts = contexts;
    }

    public String getCacheBehavior() {
        return cacheBehavior;
    }

    public List<BrowsingContext> getContexts() {
        return contexts;
    }
}
