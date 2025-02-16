package wd4j.impl.webdriver.type.browsingContext;

public class WDNavigationInfo {
    private final WDBrowsingContext context;
    private final WDNavigation WDNavigation;
    private final char timestamp;
    private final String url;

    public WDNavigationInfo(WDBrowsingContext context, WDNavigation WDNavigation, char timestamp, String url) {
        this.context = context;
        this.WDNavigation = WDNavigation;
        this.timestamp = timestamp;
        this.url = url;
    }

    public WDBrowsingContext getContext() {
        return context;
    }

    public WDNavigation getNavigation() {
        return WDNavigation;
    }

    public char getTimestamp() {
        return timestamp;
    }

    public String getUrl() {
        return url;
    }
}