package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

import java.util.List;

public class SetCacheBehaviorParameters implements Command.Params {
    private final CacheBehavior cacheBehavior;
    private final List<BrowsingContext> contexts; // Optional

    public SetCacheBehaviorParameters(CacheBehavior cacheBehavior) {
        this(cacheBehavior, null);
    }

    public SetCacheBehaviorParameters(CacheBehavior cacheBehavior, List<BrowsingContext> contexts) {
        this.cacheBehavior = cacheBehavior;
        this.contexts = contexts;
    }

    public CacheBehavior getCacheBehavior() {
        return cacheBehavior;
    }

    public List<BrowsingContext> getContexts() {
        return contexts;
    }

    public enum CacheBehavior implements EnumWrapper {
        DEFAULT("default"),
        BYPASS("bypass");

        private final String value;

        CacheBehavior(String value) {
            this.value = value;
        }

        @Override // confirmed
        public String value() {
            return value;
        }
    }
}
