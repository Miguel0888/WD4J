package wd4j.impl.webdriver.type.session;

import wd4j.impl.webdriver.type.browser.WDUserContext;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.websocket.WDCommand;

import java.util.Collections;
import java.util.List;

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
}