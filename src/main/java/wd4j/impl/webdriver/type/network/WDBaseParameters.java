package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.WDNavigation;

import java.util.List;

public class WDBaseParameters {
    private final WDBrowsingContext contextId;
    private final boolean isBlocked;
    private final WDNavigation WDNavigation;
    private final char redirectCount;
    private final WDRequestData request;
    private final long timestamp;
    private final List<WDIntercept> WDIntercepts; // optional

    public WDBaseParameters(WDBrowsingContext contextId, boolean isBlocked, WDNavigation WDNavigation, char redirectCount, WDRequestData request, long timestamp) {
        this.contextId = contextId;
        this.isBlocked = isBlocked;
        this.WDNavigation = WDNavigation;
        this.redirectCount = redirectCount;
        this.request = request;
        this.timestamp = timestamp;
        this.WDIntercepts = null;
    }

    public WDBaseParameters(WDBrowsingContext contextId, boolean isBlocked, WDNavigation WDNavigation, char redirectCount, WDRequestData request, long timestamp, List<WDIntercept> WDIntercepts) {
        this.contextId = contextId;
        this.isBlocked = isBlocked;
        this.WDNavigation = WDNavigation;
        this.redirectCount = redirectCount;
        this.request = request;
        this.timestamp = timestamp;
        this.WDIntercepts = WDIntercepts;
    }

    public WDBrowsingContext getContextId() {
        return contextId;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public WDNavigation getNavigation() {
        return WDNavigation;
    }

    public char getRedirectCount() {
        return redirectCount;
    }

    public WDRequestData getRequest() {
        return request;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<WDIntercept> getIntercepts() {
        return WDIntercepts;
    }

    public boolean hasIntercepts() {
        return WDIntercepts != null && !WDIntercepts.isEmpty();
    }
}