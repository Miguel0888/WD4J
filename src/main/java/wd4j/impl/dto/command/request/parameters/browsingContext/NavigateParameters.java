package wd4j.impl.dto.command.request.parameters.browsingContext;

import wd4j.impl.dto.type.browsingContext.WDBrowsingContext;
import wd4j.impl.dto.type.browsingContext.WDReadinessState;
import wd4j.impl.websocket.WDCommand;

public class NavigateParameters implements WDCommand.Params {
    private final WDBrowsingContext context;
    private final String url;
    private final WDReadinessState wait; // Optional

    public NavigateParameters(WDBrowsingContext context, String url) {
        this(context, url, null);
    }

    public NavigateParameters(WDBrowsingContext context, String url, WDReadinessState wait) {
        this.context = context;
        this.url = url;
        this.wait = wait;
    }

    public WDBrowsingContext getContext() {
        return context;
    }

    public String getUrl() {
        return url;
    }

    public WDReadinessState getWait() {
        return wait;
    }
}
