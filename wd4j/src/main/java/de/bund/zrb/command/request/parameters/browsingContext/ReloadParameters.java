package de.bund.zrb.command.request.parameters.browsingContext;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browsingContext.WDReadinessState;
import de.bund.zrb.api.WDCommand;

public class ReloadParameters implements WDCommand.Params {
    private final WDBrowsingContext context;
    private final Boolean ignoreCache; // optional
    private final WDReadinessState wait; // optional

    public ReloadParameters(WDBrowsingContext context) {
        this(context, null, null);
    }

    public ReloadParameters(WDBrowsingContext context, Boolean ignoreCache, WDReadinessState wait) {
        this.context = context;
        this.ignoreCache = ignoreCache;
        this.wait = wait;
    }

    public WDBrowsingContext getContext() {
        return context;
    }

    public Boolean getIgnoreCache() {
        return ignoreCache;
    }

    public WDReadinessState getWait() {
        return wait;
    }
}
