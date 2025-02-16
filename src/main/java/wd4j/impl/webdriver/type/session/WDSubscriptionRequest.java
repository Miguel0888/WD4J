package wd4j.impl.webdriver.type.session;

import wd4j.impl.webdriver.type.browser.WDUserContext;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.websocket.WDCommand;

import java.util.List;

public class WDSubscriptionRequest implements WDCommand.Params {
    private final List<String> events;
    private final List<WDBrowsingContext> contexts; // Optional
    private final List<WDUserContext> WDUserContexts; // Optional

    public WDSubscriptionRequest(List<String> events) {
        this(events, null, null);
    }

    public WDSubscriptionRequest(List<String> events, List<WDBrowsingContext> contexts, List<WDUserContext> WDUserContexts) {
        this.events = events;
        this.contexts = contexts;
        this.WDUserContexts = WDUserContexts;
    }

    public List<String> getEvents() {
        return events;
    }

    public List<WDBrowsingContext> getContexts() {
        return contexts;
    }

    public List<WDUserContext> getUserContexts() {
        return WDUserContexts;
    }
}