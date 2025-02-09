package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.network.UrlPattern;
import wd4j.impl.websocket.Command;

import java.util.List;

public class AddInterceptParameters implements Command.Params {
    private final List<InterceptPhase> phases;
    private final List<BrowsingContext> contexts;
    private final List<UrlPattern> urlPatterns;

    public AddInterceptParameters(List<InterceptPhase> phases, List<BrowsingContext> contexts, List<UrlPattern> urlPatterns) {
        this.phases = phases;
        this.contexts = contexts;
        this.urlPatterns = urlPatterns;
    }

    public List<InterceptPhase> getPhases() {
        return phases;
    }

    public List<BrowsingContext> getContexts() {
        return contexts;
    }

    public List<UrlPattern> getUrlPatterns() {
        return urlPatterns;
    }
}
