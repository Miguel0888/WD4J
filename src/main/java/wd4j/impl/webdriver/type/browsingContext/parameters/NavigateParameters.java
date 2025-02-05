package wd4j.impl.webdriver.type.browsingContext.parameters;

import wd4j.impl.webdriver.command.request.BrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.ReadinessState;
import wd4j.impl.websocket.Command;

public class NavigateParameters implements Command.Params {
    private final BrowsingContext context;
    private final String url;
    private final ReadinessState weit;

    public NavigateParameters(BrowsingContext context, String url, ReadinessState weit) {
        this.context = context;
        this.url = url;
        this.weit = weit;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public String getUrl() {
        return url;
    }

    public ReadinessState getWeit() {
        return weit;
    }
}
