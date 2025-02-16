package wd4j.impl.webdriver.type.session;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;

import java.util.List;

public class WDUnsubscribeByAttributesRequest {
    private final List<String> events;
    private final List<WDBrowsingContext> contexts; // Optional

    public WDUnsubscribeByAttributesRequest(List<String> events) {
        this(events, null);
    }

    public WDUnsubscribeByAttributesRequest(List<String> events, List<WDBrowsingContext> contexts) {
        this.events = events;
        this.contexts = contexts;
    }

    public List<String> getEvents() {
        return events;
    }

    public List<WDBrowsingContext> getContexts() {
        return contexts;
    }
}