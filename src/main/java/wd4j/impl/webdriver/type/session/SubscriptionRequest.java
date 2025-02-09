package wd4j.impl.webdriver.type.session;

import wd4j.impl.webdriver.type.browser.UserContext;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;

import java.util.List;

public class SubscriptionRequest {
    private final List<String> events;
    private final List<BrowsingContext> contexts; // Optional
    private final List<UserContext> userContexts; // Optional

    public SubscriptionRequest(List<String> events, List<BrowsingContext> contexts, List<UserContext> userContexts) {
        this.events = events;
        this.contexts = contexts;
        this.userContexts = userContexts;
    }

    public List<String> getEvents() {
        return events;
    }

    public List<BrowsingContext> getContexts() {
        return contexts;
    }

    public List<UserContext> getUserContexts() {
        return userContexts;
    }
}