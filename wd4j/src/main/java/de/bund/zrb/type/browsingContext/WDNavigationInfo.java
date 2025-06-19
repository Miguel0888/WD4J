package de.bund.zrb.type.browsingContext;

public class WDNavigationInfo {
    private final WDBrowsingContext context;
    private final WDNavigation navigation;
    private final long timestamp;
    private final String url;

    public WDNavigationInfo(WDBrowsingContext context, WDNavigation navigation, long timestamp, String url) {
        this.context = context;
        this.navigation = navigation;
        this.timestamp = timestamp;
        this.url = url;
    }

    public WDBrowsingContext getContext() {
        return context;
    }

    public WDNavigation getNavigation() {
        return navigation;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUrl() {
        return url;
    }
}