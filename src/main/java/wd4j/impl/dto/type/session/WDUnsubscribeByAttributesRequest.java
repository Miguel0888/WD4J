package wd4j.impl.dto.type.session;

import wd4j.impl.dto.type.browsingContext.WDBrowsingContext;

import java.util.List;

/**
 * This Class is ONLY useful to revert subscriptions which have NO browsing context id!
 *
 * Be careful that an unsubscribing a subscription which was made giving a context ID will not work if the context ID is not provided.
 * On the other hand, the context Parameter is deprecated and will be removed in the future. Therefore, it is recommended to use
 * unsubscribe by ID.
 */
public class WDUnsubscribeByAttributesRequest {
    private final List<String> events;
    @Deprecated // will be removed in the future,  see: https://w3c.github.io/webdriver-bidi/#type-session-UnsubscribeByAttributesRequest
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