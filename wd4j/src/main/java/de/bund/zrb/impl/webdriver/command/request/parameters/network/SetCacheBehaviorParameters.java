package de.bund.zrb.impl.webdriver.command.request.parameters.network;

import de.bund.zrb.impl.webdriver.mapping.EnumWrapper;
import de.bund.zrb.impl.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.impl.websocket.WDCommand;

import java.util.List;

public class SetCacheBehaviorParameters implements WDCommand.Params {
    private final CacheBehavior cacheBehavior;
    private final List<WDBrowsingContext> contexts; // Optional

    public SetCacheBehaviorParameters(CacheBehavior cacheBehavior) {
        this(cacheBehavior, null);
    }

    public SetCacheBehaviorParameters(CacheBehavior cacheBehavior, List<WDBrowsingContext> contexts) {
        this.cacheBehavior = cacheBehavior;
        this.contexts = contexts;
    }

    public CacheBehavior getCacheBehavior() {
        return cacheBehavior;
    }

    public List<WDBrowsingContext> getContexts() {
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
