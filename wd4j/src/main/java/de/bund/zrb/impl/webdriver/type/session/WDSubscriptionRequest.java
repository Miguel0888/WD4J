package de.bund.zrb.impl.webdriver.type.session;

import de.bund.zrb.impl.webdriver.type.browser.WDUserContext;
import de.bund.zrb.impl.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.impl.websocket.WDCommand;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WDSubscriptionRequest implements WDCommand.Params {
    private final List<String> events;
    private final List<WDBrowsingContext> contexts; // Optional
    private final List<WDUserContext> userContexts; // Optional

    // ToDo: Why this throws an exception? Contexts are optional, but practically required.
    //  *** Problem occured in Firefox 135.0.1 with an old user profile, a new user profile works fine ***
    public WDSubscriptionRequest(List<String> events) {
        this(events, null, null);
    }

    public WDSubscriptionRequest(List<String> events, List<WDBrowsingContext> contexts) {
        this(events, contexts, null);
    }

    public WDSubscriptionRequest(List<String> events, List<WDBrowsingContext> contexts, List<WDUserContext> userContexts) {
        this.events = events;
        this.contexts = contexts;
        this.userContexts = userContexts;
    }

    public WDSubscriptionRequest(String event, String browsingContextId, String userContextId) {
        this.events = Collections.singletonList(event);
        if(browsingContextId != null) {
            this.contexts = Collections.singletonList(new WDBrowsingContext(browsingContextId));
        }
        else {
            this.contexts = null;
        }
        if(userContextId != null) {
            this.userContexts = Collections.singletonList(new WDUserContext(userContextId));
        }
        else {
            this.userContexts = null;
        }
    }

    public List<String> getEvents() {
        return events;
    }

    public List<WDBrowsingContext> getContexts() {
        return contexts;
    }

    public List<WDUserContext> getUserContexts() {
        return userContexts;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Workaround, since Implementation of Subscribe & Unsubscribe by ID is not fully functional
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WDSubscriptionRequest that = (WDSubscriptionRequest) o;
        return Objects.equals(events, that.events) &&
                Objects.equals(contexts, that.contexts) &&
                Objects.equals(userContexts, that.userContexts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(events, contexts, userContexts);
    }
}