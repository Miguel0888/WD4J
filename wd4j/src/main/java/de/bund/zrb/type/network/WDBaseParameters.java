package de.bund.zrb.type.network;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browsingContext.WDNavigation;

import java.util.List;

public class WDBaseParameters {
    private final WDBrowsingContext context;
    private final boolean isBlocked;
    private final WDNavigation navigation;
    private final long redirectCount;
    private final WDRequestData request;
    private final long timestamp;
    private final List<WDIntercept> intercepts; // optional

    public WDBaseParameters(WDBrowsingContext context, boolean isBlocked, WDNavigation navigation, long redirectCount, WDRequestData request, long timestamp) {
        this.context = context;
        this.isBlocked = isBlocked;
        this.navigation = navigation;
        this.redirectCount = redirectCount;
        this.request = request;
        this.timestamp = timestamp;
        this.intercepts = null;
    }

    public WDBaseParameters(WDBrowsingContext context, boolean isBlocked, WDNavigation navigation, long redirectCount, WDRequestData request, long timestamp, List<WDIntercept> intercepts) {
        this.context = context;
        this.isBlocked = isBlocked;
        this.navigation = navigation;
        this.redirectCount = redirectCount;
        this.request = request;
        this.timestamp = timestamp;
        this.intercepts = intercepts;
    }

    public WDBrowsingContext getContext() {
        return context;
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