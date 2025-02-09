package wd4j.impl.webdriver.type.session;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;

import java.util.List;

public class UnsubscribeByAttributesRequest {
    private final List<String> events;
    private final List<BrowsingContext> contexts; // Optional

    public UnsubscribeByAttributesRequest(List<String> events, List<BrowsingContext> contexts) {
        this.events = events;
        this.contexts = contexts;
    }

    public List<String> getEvents() {
        return events;
    }

    public List<BrowsingContext> getContexts() {
        return contexts;
    }
}