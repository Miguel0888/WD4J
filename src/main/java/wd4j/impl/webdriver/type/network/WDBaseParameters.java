package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.WDNavigation;

import java.util.List;

public class WDBaseParameters {
    private final WDBrowsingContext contextId;
    private final boolean isBlocked;
    private final WDNavigation navigation;
    private final long redirectCount;
    private final WDRequestData request;
    private final long timestamp;
    private final List<WDIntercept> intercepts; // optional

    public WDBaseParameters(WDBrowsingContext contextId, boolean isBlocked, WDNavigation navigation, long redirectCount, WDRequestData request, long timestamp) {
        this.contextId = contextId;
        this.isBlocked = isBlocked;
        this.navigation = navigation;
        this.redirectCount = redirectCount;
        this.request = request;
        this.timestamp = timestamp;
        this.intercepts = null;
    }

    public WDBaseParameters(WDBrowsingContext contextId, boolean isBlocked, WDNavigation navigation, long redirectCount, WDRequestData request, long timestamp, List<WDIntercept> intercepts) {
        this.contextId = contextId;
        this.isBlocked = isBlocked;
        this.navigation = navigation;
        this.redirectCount = redirectCount;
        this.request = request;
        this.timestamp = timestamp;
        this.intercepts = intercepts;
    }

    public WDBrowsingContext getContextId() {
        return contextId;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public WDNavigation getNavigation() {
        return navigation;
    }

    public long getRedirectCount() {
        return redirectCount;
    }

    public WDRequestData getRequest() {
        return request;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<WDIntercept> getIntercepts() {
        return intercepts;
    }

    public boolean hasIntercepts() {
        return intercepts != null && !intercepts.isEmpty();
    }
}