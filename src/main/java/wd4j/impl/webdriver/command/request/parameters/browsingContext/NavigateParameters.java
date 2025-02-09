package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.command.request.BrowsingContextRequest;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.ReadinessState;
import wd4j.impl.websocket.Command;

public class NavigateParameters implements Command.Params {
    private final BrowsingContext context;
    private final String url;
    private final ReadinessState wait; // Optional

    public NavigateParameters(BrowsingContext context, String url, ReadinessState wait) {
        this.context = context;
        this.url = url;
        this.wait = wait;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public String getUrl() {
        return url;
    }

    public ReadinessState getWait() {
        return wait;
    }
}
