package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.command.request.BrowsingContextRequest;
import wd4j.impl.webdriver.type.browsingContext.ReadinessState;
import wd4j.impl.websocket.Command;

public class NavigateParameters implements Command.Params {
    private final BrowsingContextRequest context;
    private final String url;
    private final ReadinessState weit;

    public NavigateParameters(BrowsingContextRequest context, String url, ReadinessState weit) {
        this.context = context;
        this.url = url;
        this.weit = weit;
    }

    public BrowsingContextRequest getContext() {
        return context;
    }

    public String getUrl() {
        return url;
    }

    public ReadinessState getWeit() {
        return weit;
    }
}
