package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.Navigation;

import java.util.List;

public class BaseParameters {
    private final BrowsingContext contextId;
    private final boolean isBlocked;
    private final Navigation navigation;
    private final char redirectCount;
    private final RequestData request;
    private final long timestamp;
    private final List<Intercept> intercepts; // optional

    public BaseParameters(BrowsingContext contextId, boolean isBlocked, Navigation navigation, char redirectCount, RequestData request, long timestamp) {
        this.contextId = contextId;
        this.isBlocked = isBlocked;
        this.navigation = navigation;
        this.redirectCount = redirectCount;
        this.request = request;
        this.timestamp = timestamp;
        this.intercepts = null;
    }

    public BaseParameters(BrowsingContext contextId, boolean isBlocked, Navigation navigation, char redirectCount, RequestData request, long timestamp, List<Intercept> intercepts) {
        this.contextId = contextId;
        this.isBlocked = isBlocked;
        this.navigation = navigation;
        this.redirectCount = redirectCount;
        this.request = request;
        this.timestamp = timestamp;
        this.intercepts = intercepts;
    }

    public BrowsingContext getContextId() {
        return contextId;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public Navigation getNavigation() {
        return navigation;
    }

    public char getRedirectCount() {
        return redirectCount;
    }

    public RequestData getRequest() {
        return request;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<Intercept> getIntercepts() {
        return intercepts;
    }

    public boolean hasIntercepts() {
        return intercepts != null && !intercepts.isEmpty();
    }
}